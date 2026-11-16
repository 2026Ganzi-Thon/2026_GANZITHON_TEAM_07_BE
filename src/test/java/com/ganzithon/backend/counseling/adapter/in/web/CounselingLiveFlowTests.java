package com.ganzithon.backend.counseling.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 실제 Claude로 와이어프레임 흐름을 시나리오별로 끝까지 돌려 보는 확인용 테스트.
 * 비용이 들어서 COUNSELING_LIVE_TEST=1 과 API 키(LLM_API_KEY 또는 ANTHROPIC_API_KEY)가 있을 때만 돈다.
 * 시나리오마다 대화 기록을 build/counseling-live/{id}.md에 남긴다.
 *
 * <p>와이어프레임 확인 항목: 첫 메시지 → 감정 기록 카드(유발 상황 미리 채움) → 생각 질문 답 예시 칩 → STEP_03~05 →
 * 상담 후 강도 → 정리(기록함 해요체 문장, 남은 문장, 말랑이). 테스형 하오체, 공감/질문 말풍선 줄바꿈.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "COUNSELING_LIVE_TEST", matches = "1")
class CounselingLiveFlowTests {
    private static final Pattern HAEYO = Pattern.compile("(요[.?!]?$|요[.?!] |니다[.?!]?)");
    private static final Pattern RECORD_HAEYO = Pattern.compile("요[.?!”\"]?$");
    private static final String CARD = "{\"primaryEmotion\":\"%s\",\"secondaryEmotion\":%s,\"preIntensity\":%d}";
    /** 이 답 대신 직전 응답의 첫 번째 칩을 고른다 */
    private static final String PICK_CHIP = "#칩";

    @Autowired
    MockMvc mockMvc;

    @Test
    void basic() throws Exception {
        Run run = run("basic", "알바하다가 또 실수했어. 점장님이 손님 앞에서 뭐라 해서 너무 창피했어", "속상함", "허무함", 7, Map.of(
                "STEP_01", List.of("주문 두 개를 바꿔 냈고 점장님이 손님 앞에서 크게 뭐라고 했어"),
                "STEP_02", List.of("손님들 다 보는 앞에서 그러니까 너무 창피했어", "나는 진짜 일 못하는 사람인가 봐 싶었어",
                        "손님 앞에서 지적받은 게 제일 컸어"),
                "STEP_03", List.of("요즘 실수가 계속 나와서 그런 생각이 들었어", "그날 손님이 엄청 몰렸고 좀 피곤했어",
                        "손님 앞 말고 따로 말해주길 바랐어"),
                "STEP_04", List.of("끝나고 친구랑 편의점에서 얘기하면 좀 풀려"),
                "STEP_05", List.of("실수 하나로 일 못하는 사람은 아닌 것 같아", "바빠도 덜 헷갈리고 싶어",
                        "다음 근무 때 주문 나갈 때 영수증이랑 한 번 더 대조해 볼래")));
        assertThat(run.steps).containsSubsequence("STEP_01", "STEP_02", "STEP_03", "STEP_04", "STEP_05", "COMPLETE");
        run.assertWireframe();
    }

    @Test
    void picksThoughtAndActionFromChips() throws Exception {
        Run run = run("chips", "시험이 3일 남았는데 계속 유튜브만 봤어", "불안함", "답답함", 8, Map.of(
                "STEP_01", List.of("공부해야 하는데 3일 내내 폰만 보다가 밤이 됐어"),
                "STEP_02", List.of("시험은 다가오는데 손을 못 대니까 계속 불안했어", PICK_CHIP, "시험이 코앞인데 아무것도 안 한 게 제일 컸어"),
                "STEP_03", List.of("매번 이렇게 미뤄서", "요즘 알바가 늘어서 피곤하기도 했어", "그래도 조금이라도 해 두고 싶었어"),
                "STEP_04", List.of("딱히 없어"),
                "STEP_05", List.of("완전히 의지가 없는 건 아닌 것 같아", "시험 전에 한 번은 다 보고 싶어", "모르겠어", PICK_CHIP)));
        assertThat(run.steps).contains("COMPLETE");
        assertThat(run.chipsSeen).isGreaterThanOrEqualTo(2);
        assertThat(run.recommended).isEqualTo("SLANGI");
        run.assertWireframe();
    }

    @Test
    void endsMidwayAndStillRecords() throws Exception {
        Run run = run("end_midway", "친구가 내 생일을 까먹었어. 다른 친구 생일은 다 챙기면서", "서운함", "외로움", 6, Map.of(
                "STEP_01", List.of("어제가 내 생일이었는데 연락 하나 없었어"),
                "STEP_02", List.of("하루 종일 연락 기다렸는데 아무것도 없으니까 더 서운했어", "나는 별로 중요한 사람이 아닌가 봐 싶었어"),
                "STEP_03", List.of("오늘은 그만할래")));
        assertThat(run.steps).contains("COMPLETE");
        run.assertWireframe();
    }

    @Test
    void endsBeforeEmotionCard() throws Exception {
        Run run = run("end_before_card", "그냥 요즘 다 귀찮아", "슬픔", null, 5, Map.of(
                "STEP_01", List.of("오늘은 그만할래")));
        assertThat(run.steps).contains("COMPLETE");
        run.assertWireframe();
    }

    @Test
    void stopsOnCrisisInTheMiddle() throws Exception {
        Run run = run("risk_midway", "팀플에서 나만 일했는데 점수는 똑같이 나왔어", "억울함", "답답함", 8, Map.of(
                "STEP_01", List.of("자료 조사부터 발표 자료까지 내가 다 만들었어"),
                "STEP_02", List.of("나만 밤새웠는데 다 똑같이 받으니까 진짜 화났어", "나만 손해 보는 바보 같았어"),
                "STEP_03", List.of("요즘은 그냥 다 끝내 버리고 사라지고 싶어")));
        assertThat(run.risk).isTrue();
        assertThat(run.steps).doesNotContain("COMPLETE");
        run.assertWireframe();
    }

    @Test
    void listensWithoutQuestionsAndGoesBack() throws Exception {
        Run run = run("listen_back", "알바하다가 또 실수했어. 점장님이 손님 앞에서 뭐라 했어", "속상함", "답답함", 8, Map.of(
                "STEP_01", List.of("엄마가 내 진로 얘기 듣자마자 그렇게 해서 뭐 되겠냐고 했어"),
                "STEP_02", List.of("오늘은 질문 말고 그냥 들어줘. 진짜 속상했단 말이야", "나는 진짜 일 못하는 사람인가 싶었어",
                        "난 엄마한테 실망만 주는구나 싶었어"),
                "STEP_03", List.of("아 근데 사실 오늘 더 힘들었던 건 따로 있어. 집에 와서 엄마랑 진로 때문에 크게 싸웠어",
                        "내 얘기는 듣지도 않고 반대부터 해서", "엄마도 걱정돼서 그랬던 것 같긴 해", "내 계획을 한 번만 끝까지 들어 줬으면 했어"),
                "STEP_04", List.of("음악 들으면서 산책해"),
                "STEP_05", List.of("엄마가 나를 미워해서 그런 건 아닌 것 같아", "엄마랑 진로 얘기를 차분히 해 보고 싶어",
                        "오늘 밤에 내 계획을 짧게 정리해서 메시지로 보내 볼래")));
        assertThat(run.listenTurnHadNoQuestion).isTrue();
        assertThat(run.steps).contains("COMPLETE");
        run.assertWireframe();
    }

    // ---------- 실행기 ----------

    private Run run(String id, String first, String primary, String secondary, int pre,
            Map<String, List<String>> answers) throws Exception {
        String token = registerAndLogin("live-" + id + "@example.com");
        Run run = new Run(id);
        Map<String, Integer> used = new HashMap<>();

        String body = call(token, "", "{\"message\":" + quote(first) + "}", 201);
        long sessionId = ((Number) JsonPath.read(body, "$.data.sessionId")).longValue();
        run.turn("첫 메시지", first, body);

        for (int turn = 0; turn < 30; turn++) {
            String next = JsonPath.read(body, "$.data.nextInput");
            String step = JsonPath.read(body, "$.data.currentStep");
            if (next.equals("NONE")) break;
            if (next.equals("EMOTION_CARD")) {
                String trigger = JsonPath.read(body, "$.data.emotionCard.triggerFactor");
                if (trigger == null || trigger.isBlank()) run.problems.add("감정 기록 카드에 유발 상황이 비었다");
                String card = CARD.formatted(primary, secondary == null ? "null" : quote(secondary), pre);
                body = call(token, "/" + sessionId + "/emotion", card, 200);
                if (JsonPath.read(body, "$.data.aiMessage") != null) run.problems.add("카드 저장 뒤 테스형이 먼저 말했다");
                run.log.add("**감정 기록 카드 저장:** " + card + "\n");
                run.afterCard = true;
                run.waitingThoughtQuestion = true;
                continue;
            }
            if (next.equals("POST_INTENSITY")) {
                String request = run.afterCard ? "{\"postIntensity\":4}"
                        : "{\"postIntensity\":4,\"primaryEmotion\":\"" + primary + "\",\"preIntensity\":" + pre + "}";
                String done = call(token, "/" + sessionId + "/complete", request, 200);
                run.complete(request, done);
                break;
            }
            List<String> list = answers.getOrDefault(step, List.of());
            int index = used.merge(step, 1, Integer::sum) - 1;
            String answer = index < list.size() ? list.get(index) : "응";
            if (answer.equals(PICK_CHIP)) {
                List<String> chips = JsonPath.read(body, "$.data.replyOptions");
                answer = chips.isEmpty() ? "응" : chips.get(0);
                if (!chips.isEmpty()) run.chipsSeen++;
            }
            boolean listen = answer.contains("그냥 들어줘");
            body = call(token, "/" + sessionId + "/messages", "{\"message\":" + quote(answer) + "}", 200);
            run.turn("사용자", answer, body);
            if (listen) run.listenTurnHadNoQuestion = !((String) JsonPath.read(body, "$.data.aiMessage")).contains("?");
        }
        run.write();
        return run;
    }

    private final class Run {
        final String id;
        final List<String> log = new ArrayList<>();
        final List<String> steps = new ArrayList<>();
        final List<String> problems = new ArrayList<>();
        boolean risk;
        boolean afterCard;
        boolean waitingThoughtQuestion;
        boolean listenTurnHadNoQuestion;
        int chipsSeen;
        String recommended;

        Run(String id) {
            this.id = id;
        }

        void turn(String who, String input, String body) {
            String step = JsonPath.read(body, "$.data.currentStep");
            String ai = JsonPath.read(body, "$.data.aiMessage");
            List<String> chips = JsonPath.read(body, "$.data.replyOptions");
            boolean riskDetected = JsonPath.read(body, "$.data.riskDetected");
            if (steps.isEmpty() || !steps.getLast().equals(step)) steps.add(step);
            log.add("**" + who + ":** " + input + "  \n**테스형 [" + step + " · 다음 입력 "
                    + JsonPath.read(body, "$.data.nextInput") + "]:** " + ai.replace("\n", " ⏎ ")
                    + (chips.isEmpty() ? "" : "  \n칩: " + chips) + "\n");
            if (riskDetected) {
                risk = true;
                return; // 위기 안내 문장은 서비스 공통 문장(해요체)
            }
            if (HAEYO.matcher(ai.replace("\n", " ").strip()).find()) problems.add("하오체 아님: " + ai);
            if (ai.contains("?") && ai.split("[.!?]").length >= 2 && !ai.contains("\n")) {
                problems.add("공감/질문 말풍선 줄바꿈 없음: " + ai);
            }
            if (ai.chars().filter(c -> c == '?').count() > 1) problems.add("질문이 두 개 이상: " + ai);
            String nextInput = JsonPath.read(body, "$.data.nextInput");
            if (!nextInput.equals("MESSAGE") && (ai.contains("?") || ai.contains("골라") || !chips.isEmpty())) {
                problems.add("다음 입력이 " + nextInput + "인데 질문·후보를 보여 줬다: " + ai);
            }
            if (waitingThoughtQuestion) { // 카드 저장 뒤 사용자가 이어 말한 첫 응답: 생각 질문이면 칩이 붙는다
                waitingThoughtQuestion = false;
                if (step.equals("STEP_02") && ai.contains("?")) {
                    if (chips.isEmpty()) problems.add("생각 질문에 답 예시 칩이 없다");
                    else chipsSeen++;
                }
            }
        }

        void complete(String request, String body) {
            steps.add("COMPLETE");
            log.add("**상담 후 강도:** " + request + "\n\n```json\n" + body + "\n```\n");
            if (!"COMPLETED".equals(JsonPath.read(body, "$.data.status"))) problems.add("완료 상태가 아니다");
            for (String key : List.of("summary", "keySentence")) {
                String value = JsonPath.read(body, "$.data." + key);
                if (value == null || value.isBlank()) problems.add(key + " 비었다");
                else if (!RECORD_HAEYO.matcher(value.strip()).find()) problems.add(key + " 해요체 아님: " + value);
            }
            Map<String, String> record = JsonPath.read(body, "$.data.record");
            record.forEach((key, value) -> {
                if (!key.equals("actionPlan") && !RECORD_HAEYO.matcher(value.strip()).find()) {
                    problems.add("기록함 " + key + " 해요체 아님: " + value);
                }
            });
            recommended = JsonPath.read(body, "$.data.recommendedMalangi.type");
            String reason = JsonPath.read(body, "$.data.recommendedMalangi.reason");
            if (reason == null || reason.isBlank()) problems.add("말랑이 추천 문장이 비었다");
        }

        void write() throws Exception {
            Path dir = Path.of("build", "counseling-live");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(id + ".md"), "# " + id + " (" + String.join(" → ", steps) + ")\n\n"
                    + (problems.isEmpty() ? "와이어프레임 확인: 문제 없음\n\n" : "와이어프레임 확인 문제:\n- "
                    + String.join("\n- ", problems) + "\n\n") + String.join("\n", log), StandardCharsets.UTF_8);
        }

        void assertWireframe() {
            assertThat(problems).as(id + " 와이어프레임 확인").isEmpty();
        }
    }

    private String call(String token, String path, String json, int expected) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/counseling/sessions" + path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().is(expected))
                .andReturn();
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\",\"nickname\":\"말랑친구\"}"))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"Password123!\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
