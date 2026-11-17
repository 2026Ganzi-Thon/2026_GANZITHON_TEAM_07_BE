package com.ganzithon.backend.counseling.adapter.in.web;

import com.ganzithon.backend.counseling.application.port.out.CounselingAi;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/** 테스트용 LLM: 정해 둔 응답을 차례로 돌려준다. 응답이 없으면 호출 실패. */
class ScriptedCounselingAi implements CounselingAi {
    static final String REPORT_SUMMARY = "이번 달은 속상함이 자주 찾아왔지만, 스스로 다른 사정을 찾아냈구려.";
    private final java.util.concurrent.atomic.AtomicInteger reportCalls = new java.util.concurrent.atomic.AtomicInteger();
    private final Deque<ConversationReply> replies = new ArrayDeque<>();
    private final List<ConversationRequest> requests = new ArrayList<>();
    private volatile boolean risky;
    private volatile boolean safetyDown;

    synchronized void reset() {
        replies.clear();
        requests.clear();
        risky = false;
        safetyDown = false;
        reportCalls.set(0);
    }

    synchronized void push(ConversationReply reply) {
        replies.add(reply);
    }

    void risky(boolean risky) {
        this.risky = risky;
    }

    void safetyDown(boolean down) {
        this.safetyDown = down;
    }

    synchronized List<ConversationRequest> requests() {
        return List.copyOf(requests);
    }

    static ConversationReply reply(String message, boolean ready, String target, Map<String, String> extracted,
            List<String> options) {
        return new ConversationReply(message, ready, target, extracted, options, false, null);
    }

    @Override
    public synchronized ConversationReply converse(ConversationRequest request) {
        requests.add(request);
        ConversationReply next = replies.poll();
        if (next == null) throw new Unavailable("no scripted reply");
        return next;
    }

    int reportCalls() {
        return reportCalls.get();
    }

    @Override
    public ReportReply monthlyReport(String systemPrompt, String userContent) {
        reportCalls.incrementAndGet();
        return new ReportReply(REPORT_SUMMARY, List.of("실수하면 일 못하는 사람인가 싶었어요"),
                List.of("친구랑 이야기하기"), "실수 하나로 정해지지 않는다고 봤어요", "바쁠 때 메모부터 해 보면 좋을 것 같소.");
    }

    @Override
    public SafetyVerdict classify(String systemPrompt, String userContent) {
        if (safetyDown) throw new Unavailable("safety down");
        return new SafetyVerdict(risky, risky ? "SELF_HARM" : "NONE");
    }
}
