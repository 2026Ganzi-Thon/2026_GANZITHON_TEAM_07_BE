package com.ganzithon.backend.counseling.application.service;

import com.ganzithon.backend.counseling.application.port.in.CounselingFailure;
import com.ganzithon.backend.counseling.application.port.in.CounselingUseCase;
import com.ganzithon.backend.counseling.application.port.out.CounselingAi;
import com.ganzithon.backend.counseling.application.port.out.CounselingAi.ChatTurn;
import com.ganzithon.backend.counseling.application.port.out.CounselingAi.ConversationRequest;
import com.ganzithon.backend.counseling.application.port.out.CounselingSessionStore;
import com.ganzithon.backend.counseling.application.port.out.CounselingSessionStore.ResultRecord;
import com.ganzithon.backend.counseling.application.port.out.CounselingSessionStore.StoredMessage;
import com.ganzithon.backend.counseling.application.port.out.MalangiReward;
import com.ganzithon.backend.counseling.application.port.out.PromptSource;
import com.ganzithon.backend.counseling.application.service.UserSignals.ShortAnswer;
import com.ganzithon.backend.counseling.domain.CounselingSession;
import com.ganzithon.backend.counseling.domain.CounselingState;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import com.ganzithon.backend.counseling.domain.CrisisHotline;
import com.ganzithon.backend.counseling.domain.CrisisSignalFilter;
import com.ganzithon.backend.counseling.domain.EmotionCatalog;
import com.ganzithon.backend.counseling.domain.MessageSender;
import com.ganzithon.backend.counseling.domain.NextInput;
import com.ganzithon.backend.malangi.domain.MalangiType;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 상담 대화 진행 (docs/emotional-dialogue, .scratch/emotional-dialogue/issues 01~07, 와이어프레임 2026-11-14).
 *
 * <ul>
 *   <li>01: 서버가 currentStep을 관리하고 단계별 전용 프롬프트를 넣는다.</li>
 *   <li>03: 위기 1차 서버 필터 → 통과하면 2차 AI 안전 분류기를 상담 호출과 병렬로 부른다. 위기면 상담 응답을 기다리지 않는다.</li>
 *   <li>04: 최근 8턴 + 누적 슬롯으로 문맥을 만든다.</li>
 *   <li>05: AI 응답(userMessage / transitionReady / extractedData)을 검증하고, 슬롯으로 단계 전환을 판단한다.</li>
 *   <li>06: 말랑이는 대표 감정 룰로 서버가 정하고, 요약·사유는 STEP_06 호출이 쓴다.</li>
 *   <li>07: 호출 타임아웃, 1회 재시도, 최종 실패 시 단계별 기본 문장.</li>
 * </ul>
 */
public class CounselingService implements CounselingUseCase {
    public record Timeouts(Duration conversation, Duration summary, Duration safety) {}

    private static final int HISTORY_MESSAGES = 16; // 최근 8턴
    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_EMOTION_LENGTH = 20;
    private static final String SUMMARY_REQUEST = "정리해 줘";
    private static final String CARD_SKIPPED_TO_SUMMARY = "이야기해 준 덕분에 마음이 많이 정리된 것 같소.\n지금 마음의 강도를 골라 주시오.";

    private final CounselingSessionStore store;
    private final CounselingAi ai;
    private final MalangiReward reward;
    private final PromptBuilder prompts;
    private final CrisisSignalFilter crisisFilter = new CrisisSignalFilter();
    private final Clock clock;
    private final ExecutorService executor;
    private final Timeouts timeouts;

    public CounselingService(CounselingSessionStore store, CounselingAi ai, MalangiReward reward, PromptSource prompts,
            Clock clock, ExecutorService executor, Timeouts timeouts) {
        this.store = store;
        this.ai = ai;
        this.reward = reward;
        this.prompts = new PromptBuilder(prompts);
        this.clock = clock;
        this.executor = executor;
        this.timeouts = timeouts;
    }

    // ---------- 유스케이스 ----------

    @Override
    public Turn start(long userId, String message, Long malangiSessionId) {
        String text = requireMessage(message);
        if (malangiSessionId != null && !store.ownsMalangiSession(userId, malangiSessionId)) {
            throw new CounselingFailure(CounselingFailure.Code.INVALID_INPUT);
        }
        CounselingSession session = store.create(userId, malangiSessionId, now());
        return userTurn(session, text);
    }

    @Override
    public Turn send(long userId, long sessionId, String message) {
        String text = requireMessage(message);
        CounselingSession session = open(userId, sessionId);
        expect(session, NextInput.MESSAGE);
        return userTurn(session, text);
    }

    @Override
    public Turn submitEmotion(long userId, long sessionId, EmotionCard card) {
        CounselingSession session = open(userId, sessionId);
        expect(session, NextInput.EMOTION_CARD);
        CounselingState state = session.state();
        recordEmotionCard(state, card, true);
        if (card.triggerFactor() != null && !card.triggerFactor().isBlank()) {
            state.putSlot("triggerFactor", limit(card.triggerFactor().strip(), 500)); // 카드에서 고친 유발 상황
        }

        CounselingStep target = CounselingStep.STEP_02;
        while (target != CounselingStep.STEP_06 && StepTargets.stepDone(target, state)) target = target.next();
        if (target == CounselingStep.STEP_06) {
            state.nextInput(NextInput.POST_INTENSITY);
            return finish(session, CARD_SKIPPED_TO_SUMMARY, List.of());
        }
        // 와이어프레임: 카드는 대화에 "감정 기록 저장됨"으로 남고, 테스형은 사용자가 이어서 말한 뒤 생각을 묻는다
        session.moveTo(target);
        state.nextInput(NextInput.MESSAGE);
        state.cardFresh(true);
        String emotions = state.secondaryEmotion() == null ? state.primaryEmotion()
                : state.primaryEmotion() + ", " + state.secondaryEmotion();
        String cardText = "(감정 기록: " + emotions + " / 강도 " + state.preIntensity() + ")";
        long messageId = store.appendMessage(session.id(), target, MessageSender.SYSTEM, cardText, false, now());
        state.addCard(messageId);
        store.save(session);
        return new Turn(session.id(), session.status(), session.step(), null, false, List.of(), List.of(),
                NextInput.MESSAGE, null);
    }

    @Override
    public Completion complete(long userId, long sessionId, int postIntensity, EmotionCard emotionIfMissing) {
        CounselingSession session = open(userId, sessionId);
        expect(session, NextInput.POST_INTENSITY);
        if (postIntensity < 0 || postIntensity > 10) throw new CounselingFailure(CounselingFailure.Code.INVALID_INPUT);
        CounselingState state = session.state();
        if (state.primaryEmotion() == null) {
            // 감정 기록 카드 전에 상담을 마친 경우: 완료 요청에서 감정을 받는다
            if (emotionIfMissing == null) throw new CounselingFailure(CounselingFailure.Code.EMOTION_REQUIRED);
            recordEmotionCard(state, emotionIfMissing, true);
        }
        state.recordPostIntensity(postIntensity);
        MalangiType recommended = EmotionCatalog.malangiFor(state.primaryEmotion());

        List<ChatTurn> history = history(session.id());
        session.moveTo(CounselingStep.STEP_06);
        long messageId = store.appendMessage(session.id(), CounselingStep.STEP_06, MessageSender.SYSTEM,
                SUMMARY_REQUEST, false, now());
        Draft d = converse(session, history, SUMMARY_REQUEST, List.of(), false, recommended);

        String aiMessage;
        String summary;
        String keySentence;
        String reason;
        Map<String, String> record = new LinkedHashMap<>();
        for (String key : List.of("initialThought", "alternativeThought", "actionPlan")) {
            String slot = state.slot(key);
            if (!ReplyRules.notEmpty(slot)) continue;
            String polished = d == null ? null : d.extracted(key);
            record.put(key, ReplyRules.notEmpty(polished) ? polished : slot); // 정리는 확인된 슬롯만 다듬는다
        }
        if (d == null) {
            aiMessage = FallbackMessages.SUMMARY;
            summary = state.slot("situation") == null ? "" : state.slot("situation");
            keySentence = "";
            reason = "";
        } else {
            aiMessage = d.userMessage;
            summary = nz(d.extracted("summary"));
            keySentence = nz(d.extracted("keySentence"));
            reason = nz(d.extracted("malangiReason"));
        }
        state.recordSummary(summary, keySentence, reason, record);

        boolean newlyUnlocked = reward.unlock(userId, recommended);
        Integer change = state.preIntensity() == null ? null : postIntensity - state.preIntensity();
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("triggerMessageId", messageId);
        entry.put("step", CounselingStep.STEP_06.name());
        entry.put("intensity", postIntensity);
        entry.put("intensityChange", change);
        entry.put("recommendedMalangi", recommended.name());
        entry.put("summary", summary);
        entry.put("keySentence", keySentence);
        entry.put("malangiReason", reason);
        entry.put("secondaryEmotion", state.secondaryEmotion());
        entry.put("record", record);
        for (String key : List.of("triggerFactor", "underlyingNeed", "copingStrategy")) {
            entry.put(key, ReplyRules.notEmpty(state.slot(key)) ? state.slot(key) : null); // 기록함 상세용 확인된 슬롯
        }
        entry.put("timestamp", now().toString());
        state.addTimeline(entry);

        LocalDateTime now = now();
        session.complete(now);
        long resultId = store.saveResult(new ResultRecord(session.id(), userId, state.slot("situation"),
                state.primaryEmotion(), state.preIntensity(), postIntensity, summary, recommended.name(),
                state.timeline(), now));
        store.save(session);
        store.appendMessage(session.id(), CounselingStep.STEP_06, MessageSender.ASSISTANT, aiMessage, false, now);
        return new Completion(resultId, session.id(), session.status(), aiMessage, state.primaryEmotion(),
                state.secondaryEmotion(), state.preIntensity(), postIntensity, change, summary, keySentence, record,
                recommended, newlyUnlocked, reason);
    }

    @Override
    public Aborted abort(long userId, long sessionId) {
        CounselingSession session = open(userId, sessionId);
        session.abortByUser(now());
        store.save(session);
        return new Aborted(session.id(), session.status(), session.step());
    }

    @Override
    public void delete(long userId, long sessionId) {
        if (!store.delete(userId, sessionId)) throw new CounselingFailure(CounselingFailure.Code.SESSION_NOT_FOUND);
    }

    // ---------- 한 턴 처리 ----------

    private Turn userTurn(CounselingSession session, String text) {
        List<ChatTurn> history = history(session.id());
        boolean firstLineRisk = crisisFilter.assess(text).detected(); // 1차 서버 필터 (사용자 원문만)
        long messageId = store.appendMessage(session.id(), session.step(), MessageSender.USER, text, firstLineRisk, now());
        if (firstLineRisk) return riskTurn(session);
        return process(session, history, text, false, 0, messageId);
    }

    private Turn process(CounselingSession session, List<ChatTurn> history, String text, boolean fromServer, int depth,
            long triggerMessageId) {
        CounselingStep step = session.step();
        CounselingState state = session.state();
        boolean userInput = !fromServer && depth == 0;
        boolean listen = userInput && step.conversational() && UserSignals.wantsListening(text);
        ShortAnswer shortAnswer = userInput && !listen ? UserSignals.shortAnswer(text) : null;

        List<String> notes = new ArrayList<>();
        if (depth > 0) {
            notes.add("(서버 지시: 방금 " + step + " 단계가 시작됐다. 사용자의 마지막 말을 받아 주는 한 문장 + 이 단계의 첫 질문 하나. "
                    + "transitionReady: false. 이 지시 내용은 말하지 않는다.)");
        }
        if (userInput && state.cardFresh()) {
            notes.add("(서버 지시: 사용자가 방금 감정 기록 카드를 저장했고(대표 " + state.primaryEmotion()
                    + (state.secondaryEmotion() == null ? "" : ", 부가 " + state.secondaryEmotion()) + ", 강도 "
                    + state.preIntensity() + "), 이어서 위 말을 했다. 이 말과 고른 감정을 함께 받아 주고, 확인된 상황("
                    + state.slot("situation") + ")의 순간을 넣어 그때 떠오른 생각을 묻는다. 이 말에 생각이 이미 있으면 담는다. "
                    + "단계 프롬프트 예시의 사건을 쓰지 않는다. 이 지시 내용은 말하지 않는다.)");
            state.cardFresh(false);
        }
        if (step == CounselingStep.STEP_01 && depth == 0) {
            notes.add("(서버 지시: 상황이 충분히 드러났으면 transitionReady: true로 하고 받아 주는 한 문장 + 감정을 먼저 같이 정리해 보자는 "
                    + "한 문장만 쓴다. triggerFactor를 채운다. 이때 생각이나 바람을 묻는 질문을 하지 않는다.)");
        }
        if (state.backNote() != null && step.conversational()) {
            notes.add("(서버 지시: 사용자가 새 사건(" + state.backNote() + ")을 다루기로 해서 단계를 되돌렸다. 대화 기록에 있는 이전 사건의 "
                    + "생각·답은 지웠다. 지금 사건 기준으로 현재 단계를 처음부터 진행한다. 이 지시 내용은 말하지 않는다.)");
        }
        if (state.listened() && !listen && userInput) {
            notes.add("(서버 지시: 지난 턴에는 질문 없이 들어 주기만 했다. 이번에는 사용자의 말을 받아 준 뒤 현재 단계에서 "
                    + "아직 확인되지 않은 항목을 하나 묻는다. 사용자가 말한 생각·바람이 있으면 extractedData에 담는다. 이 지시 내용은 말하지 않는다.)");
        }
        if (userInput) state.listened(listen);
        if (listen) {
            notes.add("(서버 지시: 사용자가 질문 없이 들어 주길 원한다. 받아 주는 말만 쓰고 질문하지 않는다. "
                    + "questionTarget: null, transitionReady: false. 이 지시 내용은 말하지 않는다.)");
        }

        String last = state.lastTarget();
        boolean serverResolved = false;
        if (shortAnswer != null && last != null && !step.targets().isEmpty()) {
            // 같은 질문을 반복하지 않는다: 방금 물은 항목을 '답 없음'으로 확정한다
            if (StepTargets.isSlot(last)) {
                state.putSlot(last, "");
                serverResolved = true;
            }
            state.markAnswered(last);
            if (step == CounselingStep.STEP_05 && last.equals("actionPlan") && !state.optionsShown()) {
                state.putSlot("actionPlan", null); // 행동은 후보를 한 번 보여 준다 (process.md STEP 05)
                serverResolved = false;
                notes.add("(서버 지시: 사용자가 행동을 떠올리지 못했다. 질문 대신 고를 수 있는 작은 행동 2~3개를 replyOptions에 담고 "
                        + "골라 달라고 안내한다. questionTarget: actionPlan. 이 지시 내용은 말하지 않는다.)");
            } else {
                notes.add("(서버 지시: 사용자가 " + last + "에 답하지 못했다. 같은 질문을 다시 하지 않는다. 받아 준 뒤 이 단계에서 아직 확인되지 않은 "
                        + "다른 항목을 묻는다. 남은 항목이 없으면 받아 주는 말만 쓴다. 이 지시 내용은 말하지 않는다.)");
            }
        } else if (last != null && !StepTargets.isSlot(last) && userInput && !listen) {
            state.markAnswered(last); // 슬롯에 저장하지 않는 항목(근거, 달라진 부분, 원하는 상황)은 답하면 확인된 것으로 본다
        }

        // 상담 호출과 2차 안전 분류기를 병렬로. 위기면 상담 응답을 기다리지 않는다.
        Future<Boolean> safety = userInput ? executor.submit(() -> classify(history, text)) : null;
        Future<Draft> conversation = executor.submit(() -> converse(session, history, text, notes, listen, null));
        if (safety != null) {
            Boolean risk = await(safety);
            if (risk == null) { // 분류기 최종 실패 → 안전 기본 문장 (Fail-Safe)
                conversation.cancel(true);
                return finish(session, FallbackMessages.SAFETY_CHECK_FAILED, List.of());
            }
            if (risk) {
                conversation.cancel(true);
                store.markMessageRisk(triggerMessageId);
                return riskTurn(session);
            }
        }
        Draft d = await(conversation);
        if (d == null) {
            if (depth > 0) {
                state.lastTarget(StepTargets.next(step, state)); // 새 단계 첫 질문을 기본 문장으로 물었다
                return finish(session, FallbackMessages.start(step), List.of());
            }
            return finish(session, FallbackMessages.retry(step), List.of());
        }
        return apply(session, history, text, fromServer, depth, triggerMessageId, d, serverResolved, listen);
    }

    private Turn apply(CounselingSession session, List<ChatTurn> history, String text, boolean fromServer, int depth,
            long triggerMessageId, Draft d, boolean serverResolved, boolean listen) {
        CounselingStep step = session.step();
        CounselingState state = session.state();
        d.userMessage = ReplyRules.splitBubble(d.userMessage);
        if (!d.replyOptions.isEmpty()) state.optionsShown(true);
        mergeSlots(state, step, d.extracted);
        state.lastTarget(d.asks() || !d.replyOptions.isEmpty() ? d.questionTarget : null);

        if (d.endRequested) {
            state.endRequested(true);
            state.nextInput(NextInput.POST_INTENSITY);
            return finish(session, d.userMessage, List.of());
        }

        CounselingStep back = ReplyRules.parseStep(d.backToStep);
        if (back != null && step.canGoBackTo(back) && depth == 0) {
            if (back == CounselingStep.STEP_01) {
                state.backNote(limit(text, 60));
                state.clearSlots();
                state.clearEmotions();
            } else {
                state.putSlot("underlyingNeed", null);
            }
            session.moveTo(back);
            return process(session, history, text, fromServer, depth + 1, triggerMessageId);
        }

        if (serverResolved && !listen && StepTargets.stepDone(step, state)) {
            // 서버가 '답 없음'으로 단계를 끝냈다 → 다시 묻거나 후보를 권하는 줄은 버리고 받아 주는 말만 남긴다
            if (d.userMessage.contains("\n")) d.userMessage = d.userMessage.split("\n")[0].strip();
            d.replyOptions = new ArrayList<>();
        }
        // 전환: 필수 슬롯이 확인됐고, AI가 전환을 추천했거나 서버가 '답 없음'으로 확정했을 때 (05번 이슈)
        if (listen || !StepTargets.stepDone(step, state) || !(d.transitionReady || serverResolved)) {
            return finish(session, d.userMessage, d.replyOptions);
        }
        addStepTimeline(state, step, triggerMessageId);
        if (step == CounselingStep.STEP_01) {
            state.nextInput(NextInput.EMOTION_CARD); // 대화 안 감정 기록 카드
            return finish(session, d.userMessage, List.of());
        }
        CounselingStep next = step.next();
        while (next != CounselingStep.STEP_06 && StepTargets.stepDone(next, state)) next = next.next();
        if (next == CounselingStep.STEP_06) {
            state.nextInput(NextInput.POST_INTENSITY); // 상담 후 강도 → 정리
            return finish(session, d.userMessage, List.of());
        }
        session.moveTo(next);
        if (depth < 1) {
            return process(session, history, text, fromServer, depth + 1, triggerMessageId); // 새 단계 첫 질문을 받아 온다
        }
        return finish(session, d.userMessage, d.replyOptions);
    }

    private static void mergeSlots(CounselingState state, CounselingStep step, Map<String, String> extracted) {
        for (String key : CounselingState.SLOT_KEYS) {
            String value = extracted.get(key);
            if (value == null) continue;
            String old = state.slot(key);
            if (value.isEmpty() && ReplyRules.notEmpty(old)) continue; // 이미 값이 있으면 빈 값으로 덮지 않는다
            if (key.equals("actionPlan") && step != CounselingStep.STEP_05) continue; // 실천 행동은 STEP_05에서 고른다
            if (key.equals("alternativeThought") && step == CounselingStep.STEP_05 && ReplyRules.notEmpty(old)
                    && !value.contains(old)) {
                value = old + " / " + value; // STEP_05에서 달라진 관점은 STEP_03 관점 뒤에 이어 붙인다
            }
            state.putSlot(key, value);
        }
    }

    private void addStepTimeline(CounselingState state, CounselingStep step, long triggerMessageId) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("triggerMessageId", triggerMessageId);
        entry.put("step", step.name());
        switch (step) {
            case STEP_01 -> {
                entry.put("situation", state.slot("situation"));
                entry.put("triggerFactor", state.slot("triggerFactor"));
            }
            case STEP_02 -> {
                entry.put("emotion", state.primaryEmotion());
                entry.put("secondaryEmotion", state.secondaryEmotion());
                entry.put("intensity", state.preIntensity());
                entry.put("thought", state.slot("initialThought"));
                entry.put("triggerFactor", state.slot("triggerFactor"));
            }
            case STEP_03 -> {
                entry.put("alternativeThought", state.slot("alternativeThought"));
                entry.put("underlyingNeed", state.slot("underlyingNeed"));
            }
            case STEP_04 -> entry.put("copingStrategy", state.slot("copingStrategy"));
            case STEP_05 -> {
                entry.put("alternativeThought", state.slot("alternativeThought"));
                entry.put("actionPlan", state.slot("actionPlan"));
            }
            default -> { }
        }
        entry.put("timestamp", now().toString());
        state.addTimeline(entry);
    }

    // ---------- LLM 호출 ----------

    /** 상담 호출 + 검증, 규칙 위반이나 실패면 1회 다시. 최종 실패면 null. */
    private Draft converse(CounselingSession session, List<ChatTurn> history, String text, List<String> notes,
            boolean listen, MalangiType recommended) {
        CounselingStep step = session.step();
        CounselingState state = session.state();
        PromptBuilder.Built built = prompts.conversation(step, state, recommended);
        String content = notes.isEmpty() ? text : text + "\n\n" + String.join("\n", notes);
        Duration timeout = step == CounselingStep.STEP_06 ? timeouts.summary() : timeouts.conversation();
        List<ChatTurn> window = history.size() > HISTORY_MESSAGES
                ? history.subList(history.size() - HISTORY_MESSAGES, history.size()) : history;
        for (int attempt = 0; attempt < 2; attempt++) {
            String userContent = content;
            try {
                var reply = withTimeout(() -> ai.converse(new ConversationRequest(built.cachedSystem(),
                        built.stateSystem(), window, userContent, step == CounselingStep.STEP_06)), timeout);
                Draft d = new Draft(reply);
                String problem = ReplyRules.check(d, step, state, listen, recommended);
                if (problem == null || attempt == 1) return d; // 다시 받은 응답은 보정만 하고 쓴다
                content = content + "\n(서버 지시: 이전 응답이 규칙에 맞지 않았다 — " + problem + ". 규칙에 맞게 다시 쓴다.)";
            } catch (CounselingAi.Unavailable exception) {
                pause(300);
            }
        }
        return null;
    }

    /** 2차 AI 안전 분류. 두 번 다 실패하면 null. */
    private Boolean classify(List<ChatTurn> history, String text) {
        List<ChatTurn> recent = history.size() > 4 ? history.subList(history.size() - 4, history.size()) : history;
        StringBuilder context = new StringBuilder();
        for (ChatTurn turn : recent) {
            if (!context.isEmpty()) context.append('\n');
            context.append(turn.fromUser() ? "사용자: " : "AI: ").append(turn.text());
        }
        String content = "[직전 대화]\n" + (context.isEmpty() ? "(없음)" : context) + "\n\n[판단할 사용자 메시지]\n" + text;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return withTimeout(() -> ai.classify(prompts.safetySystem(), content), timeouts.safety()).riskFlag();
            } catch (CounselingAi.Unavailable exception) {
                pause(500L * (attempt + 1));
            }
        }
        return null;
    }

    private <T> T withTimeout(Callable<T> call, Duration timeout) {
        Future<T> future = executor.submit(call);
        try {
            return future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException exception) {
            future.cancel(true);
            throw new CounselingAi.Unavailable("timed out after " + timeout.toMillis() + "ms", exception);
        } catch (ExecutionException exception) {
            throw new CounselingAi.Unavailable("call failed", exception.getCause());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new CounselingAi.Unavailable("interrupted", exception);
        }
    }

    private static <T> T await(Future<T> future) {
        try {
            return future.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException exception) {
            return null;
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    // ---------- 공통 ----------

    private Turn finish(CounselingSession session, String aiMessage, List<String> replyOptions) {
        store.save(session);
        store.appendMessage(session.id(), session.step(), MessageSender.ASSISTANT, aiMessage, false, now());
        CounselingState state = session.state();
        NextInput next = state.nextInput();
        EmotionCardPrompt card = next == NextInput.EMOTION_CARD
                ? new EmotionCardPrompt(state.slot("triggerFactor"), EmotionCatalog.PRIMARY, EmotionCatalog.SECONDARY)
                : null;
        return new Turn(session.id(), session.status(), session.step(), aiMessage, false, List.of(),
                List.copyOf(replyOptions), next, card);
    }

    private Turn riskTurn(CounselingSession session) {
        session.abortByRisk(now());
        store.save(session);
        store.appendMessage(session.id(), session.step(), MessageSender.ASSISTANT, CrisisHotline.GUIDANCE_MESSAGE,
                false, now());
        return new Turn(session.id(), session.status(), session.step(), CrisisHotline.GUIDANCE_MESSAGE, true,
                List.of(CrisisHotline.values()), List.of(), NextInput.NONE, null);
    }

    /** 최근 대화. 사용자 쪽 말이 이어지면(감정 기록 카드 + 사용자 말) 한 턴으로 합친다. */
    private List<ChatTurn> history(long sessionId) {
        List<ChatTurn> turns = new ArrayList<>();
        for (StoredMessage message : store.recentMessages(sessionId, HISTORY_MESSAGES)) {
            boolean fromUser = message.sender() != MessageSender.ASSISTANT;
            if (!turns.isEmpty() && turns.getLast().fromUser() && fromUser) {
                turns.set(turns.size() - 1, new ChatTurn(true, turns.getLast().text() + "\n" + message.content()));
            } else {
                turns.add(new ChatTurn(fromUser, message.content()));
            }
        }
        return turns;
    }

    private CounselingSession open(long userId, long sessionId) {
        CounselingSession session = store.find(userId, sessionId)
                .orElseThrow(() -> new CounselingFailure(CounselingFailure.Code.SESSION_NOT_FOUND));
        if (!session.inProgress()) throw new CounselingFailure(CounselingFailure.Code.SESSION_CLOSED);
        return session;
    }

    private static void expect(CounselingSession session, NextInput input) {
        if (session.state().nextInput() != input) {
            throw new CounselingFailure(CounselingFailure.Code.INPUT_NOT_EXPECTED);
        }
    }

    private static void recordEmotionCard(CounselingState state, EmotionCard card, boolean requireIntensity) {
        if (card == null) throw new CounselingFailure(CounselingFailure.Code.EMOTION_REQUIRED);
        String primary = card.primaryEmotion() == null ? "" : card.primaryEmotion().strip();
        String secondary = card.secondaryEmotion() == null ? "" : card.secondaryEmotion().strip();
        Integer pre = card.preIntensity();
        if (primary.isEmpty() || (requireIntensity && pre == null)) {
            throw new CounselingFailure(CounselingFailure.Code.EMOTION_REQUIRED);
        }
        if (primary.length() > MAX_EMOTION_LENGTH || secondary.length() > MAX_EMOTION_LENGTH
                || (pre != null && (pre < 0 || pre > 10))) {
            throw new CounselingFailure(CounselingFailure.Code.INVALID_INPUT);
        }
        state.recordEmotions(primary, secondary.isEmpty() || secondary.equals(primary) ? null : secondary, pre);
    }

    private static String requireMessage(String message) {
        if (message == null || message.isBlank() || message.length() > MAX_MESSAGE_LENGTH) {
            throw new CounselingFailure(CounselingFailure.Code.INVALID_INPUT);
        }
        return message.strip();
    }

    private static String limit(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max);
    }

    private static String nz(String value) {
        return value == null ? "" : value;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
