package com.ganzithon.backend.counseling.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
 * 실제 Claude로 와이어프레임 흐름을 끝까지 한 번 돌려 보는 확인용 테스트.
 * 비용이 들어서 COUNSELING_LIVE_TEST=1 과 API 키(LLM_API_KEY 또는 ANTHROPIC_API_KEY)가 있을 때만 돈다.
 * 대화 기록은 build/counseling-live-flow.md에 남긴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "COUNSELING_LIVE_TEST", matches = "1")
class CounselingLiveFlowTests {
    /** 단계마다 물을 순서대로 준비한 가상 사용자 답 (eval/scenarios.json basic과 같은 설정). */
    private static final Map<String, List<String>> ANSWERS = Map.of(
            "STEP_01", List.of("주문 두 개를 바꿔 냈고 점장님이 손님 앞에서 크게 뭐라고 했어"),
            "STEP_02", List.of("나는 진짜 일 못하는 사람인가 봐 싶었어", "손님 앞에서 지적받은 게 제일 컸어"),
            "STEP_03", List.of("요즘 실수가 계속 나와서 그런 생각이 들었어", "그날 손님이 엄청 몰렸고 좀 피곤했어",
                    "손님 앞 말고 따로 말해주길 바랐어"),
            "STEP_04", List.of("끝나고 친구랑 편의점에서 얘기하면 좀 풀려"),
            "STEP_05", List.of("실수 하나로 일 못하는 사람은 아닌 것 같아", "바빠도 덜 헷갈리고 싶어",
                    "다음 근무 때 주문 나갈 때 영수증이랑 한 번 더 대조해 볼래", "응 그거 해 볼게"));

    @Autowired
    MockMvc mockMvc;

    @Test
    void runsWireframeFlowWithRealModel() throws Exception {
        String token = registerAndLogin("counsel-live@example.com");
        List<String> log = new ArrayList<>();
        List<String> steps = new ArrayList<>();

        String first = "알바하다가 또 실수했어. 점장님이 손님 앞에서 뭐라 해서 너무 창피했어";
        String body = call(token, "", "{\"message\":" + quote(first) + "}", 201);
        long sessionId = ((Number) JsonPath.read(body, "$.data.sessionId")).longValue();
        record(log, steps, "첫 메시지", first, body);

        String lastStep = "STEP_01";
        int index = 0;
        for (int turn = 0; turn < 25; turn++) {
            String next = JsonPath.read(body, "$.data.nextInput");
            String step = JsonPath.read(body, "$.data.currentStep");
            if (!step.equals(lastStep)) {
                lastStep = step;
                index = 0;
            }
            if (next.equals("NONE")) break;
            if (next.equals("EMOTION_CARD")) {
                String card = "{\"primaryEmotion\":\"속상함\",\"secondaryEmotion\":\"허무함\",\"preIntensity\":7}";
                body = call(token, "/" + sessionId + "/emotion", card, 200);
                record(log, steps, "감정 기록 카드", card, body);
                continue;
            }
            if (next.equals("POST_INTENSITY")) {
                String done = call(token, "/" + sessionId + "/complete", "{\"postIntensity\":4}", 200);
                log.add("**상담 후 강도 4 → 정리**\n\n```json\n" + done + "\n```\n");
                steps.add("COMPLETE");
                body = done;
                assertThat((String) JsonPath.read(done, "$.data.status")).isEqualTo("COMPLETED");
                assertThat((String) JsonPath.read(done, "$.data.summary")).isNotBlank();
                assertThat((String) JsonPath.read(done, "$.data.keySentence")).isNotBlank();
                break;
            }
            List<String> answers = ANSWERS.getOrDefault(step, List.of("응"));
            String answer = index < answers.size() ? answers.get(index) : "응";
            index++;
            body = call(token, "/" + sessionId + "/messages", "{\"message\":" + quote(answer) + "}", 200);
            record(log, steps, "사용자", answer, body);
        }

        Files.writeString(Path.of("build", "counseling-live-flow.md"),
                "# 실제 모델 흐름 (" + String.join(" → ", steps) + ")\n\n" + String.join("\n", log),
                StandardCharsets.UTF_8);
        assertThat(steps).contains("STEP_02", "STEP_03", "STEP_05", "COMPLETE");
    }

    private static void record(List<String> log, List<String> steps, String who, String input, String body) {
        String step = JsonPath.read(body, "$.data.currentStep");
        if (steps.isEmpty() || !steps.getLast().equals(step)) steps.add(step);
        Object options = JsonPath.read(body, "$.data.replyOptions");
        log.add("**" + who + ":** " + input + "  \n**테스형 [" + step + ", 다음 입력 " + JsonPath.read(body, "$.data.nextInput")
                + "]:** " + ((String) JsonPath.read(body, "$.data.aiMessage")).replace("\n", " / ")
                + (options instanceof List<?> list && !list.isEmpty() ? "  \n칩: " + list : "") + "\n");
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
