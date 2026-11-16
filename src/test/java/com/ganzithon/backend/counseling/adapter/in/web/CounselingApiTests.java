package com.ganzithon.backend.counseling.adapter.in.web;

import static com.ganzithon.backend.counseling.adapter.in.web.ScriptedCounselingAi.reply;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CounselingApiTests {

    @TestConfiguration
    static class ScriptedAiConfig {
        @Bean
        @Primary
        ScriptedCounselingAi scriptedCounselingAi() {
            return new ScriptedCounselingAi();
        }
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ScriptedCounselingAi ai;

    @BeforeEach
    void resetAi() {
        ai.reset();
    }

    @Test
    void followsWireframeFlowFromFirstMessageToRecord() throws Exception {
        String token = registerAndLogin("counsel-flow@example.com");

        // STEP_01: 첫 메시지 → 상황이 나오면 감정 기록 카드
        ai.push(reply("손님 앞에서 지적을 받아 많이 창피했을 것 같소.\n그때의 감정을 먼저 같이 정리해 보면 좋을 것 같소.", true, null,
                Map.of("situation", "알바 중 주문을 바꿔 내서 점장님이 손님 앞에서 지적함",
                        "triggerFactor", "손님 앞에서 점장님에게 지적받음"), List.of()));
        MvcResult started = start(token, "알바하다가 주문 두 개를 바꿔 냈는데 점장님이 손님 앞에서 뭐라 했어")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.currentStep").value("STEP_01"))
                .andExpect(jsonPath("$.data.nextInput").value("EMOTION_CARD"))
                .andExpect(jsonPath("$.data.emotionCard.triggerFactor").value("손님 앞에서 점장님에게 지적받음"))
                .andExpect(jsonPath("$.data.emotionCard.primaryEmotions.length()").value(6))
                .andExpect(jsonPath("$.data.riskDetected").value(false))
                .andReturn();
        long sessionId = ((Number) JsonPath.read(started.getResponse().getContentAsString(), "$.data.sessionId")).longValue();

        // 감정 기록 카드 → STEP_02 첫 질문 + 답 예시 칩
        ai.push(reply("속상하고 허무한 마음이 함께 있었구려.\n그 순간 머릿속에 가장 먼저 스친 생각은 무엇이었소?", false, "initialThought",
                Map.of(), List.of("난 일을 제대로 못 하는 사람인가 싶었다", "다들 나를 한심하게 볼 것 같았다")));
        call(token, "/" + sessionId + "/emotion",
                "{\"primaryEmotion\":\"속상함\",\"secondaryEmotion\":\"허무함\",\"preIntensity\":7}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentStep").value("STEP_02"))
                .andExpect(jsonPath("$.data.replyOptions.length()").value(2))
                .andExpect(jsonPath("$.data.nextInput").value("MESSAGE"));
        assertThat(ai.requests().getLast().userText()).startsWith("(감정 기록: 속상함, 허무함 / 강도 7)");

        // STEP_02 완료 → 서버가 STEP_03 첫 질문을 다시 받아 온다
        ai.push(reply("일 못하는 사람이라는 생각까지 들었구려.", true, null, Map.of("initialThought", "나는 일 못하는 사람인가 봐"), List.of()));
        ai.push(reply("그런 생각이 들 만큼 마음이 무거웠구려.\n'일 못하는 사람인가 봐'라는 생각이 든 이유는 무엇이오?", false, "thoughtReason",
                Map.of(), List.of()));
        send(token, sessionId, "나는 일 못하는 사람인가 봐 싶었어")
                .andExpect(jsonPath("$.data.currentStep").value("STEP_03"))
                .andExpect(jsonPath("$.data.aiMessage").value("그런 생각이 들 만큼 마음이 무거웠구려.\n'일 못하는 사람인가 봐'라는 생각이 든 이유는 무엇이오?"));
        assertThat(ai.requests().getLast().stateSystem()).contains("현재 단계: STEP_03").contains("이번에 물을 항목 (서버가 계산): thoughtReason");

        ai.push(reply("요즘 실수가 이어져 그렇게 느꼈구려.\n그날 실수가 나온 데 다른 이유나 사정이 있었소?", false, "alternativeThought",
                Map.of(), List.of()));
        send(token, sessionId, "요즘 실수가 계속 나와서").andExpect(jsonPath("$.data.currentStep").value("STEP_03"));

        ai.push(reply("손님이 몰렸던 날이었구려.\n지적을 받은 순간 사실 가장 바랐던 것은 무엇이었소?", false, "underlyingNeed",
                Map.of("alternativeThought", "그날 손님이 몰렸고 피곤했다"), List.of()));
        send(token, sessionId, "그날 손님이 몰렸고 좀 피곤했어");

        // STEP_03 완료 → STEP_04
        ai.push(reply("따로 말해 주길 바랐구려.", true, null, Map.of("underlyingNeed", "손님 앞 말고 따로 말해주길 바랐다"), List.of()));
        ai.push(reply("그 마음이 컸구려.\n이렇게 속상할 때 지금까지 어떻게 버텨 왔소?", false, "copingStrategy", Map.of(), List.of()));
        send(token, sessionId, "손님 앞 말고 따로 말해주길 바랐어").andExpect(jsonPath("$.data.currentStep").value("STEP_04"));

        // STEP_04 완료 → STEP_05
        ai.push(reply("친구와 이야기하며 풀어 왔구려.", true, null, Map.of("copingStrategy", "친구랑 편의점에서 이야기하기"), List.of()));
        ai.push(reply("그렇게 버텨 왔구려.\n처음 생각과 비교했을 때 지금 달라진 부분이 있소?", false, "changedView", Map.of(), List.of()));
        send(token, sessionId, "끝나고 친구랑 편의점에서 얘기하면 풀려").andExpect(jsonPath("$.data.currentStep").value("STEP_05"));

        ai.push(reply("실수 하나로 다 정해지진 않는다고 보게 됐구려.\n앞으로 어떤 상황이 되었으면 좋겠소?", false, "desiredSituation",
                Map.of("alternativeThought", "실수 하나로 일 못하는 사람은 아니다"), List.of()));
        send(token, sessionId, "실수 하나로 일 못하는 사람은 아닌 것 같아");

        ai.push(reply("바빠도 덜 헷갈리고 싶구려.\n그걸 위해 지금 해 볼 수 있는 작은 행동은 무엇이 있겠소?", false, "actionPlan",
                Map.of(), List.of()));
        send(token, sessionId, "바빠도 덜 헷갈리고 싶어");

        // STEP_05 완료 → 상담 후 강도 입력
        ai.push(reply("다음 근무 때 영수증과 한 번 더 대조해 보기로 했구려.", true, null,
                Map.of("actionPlan", "다음 근무 때 주문 나갈 때 영수증이랑 한 번 더 대조하기"), List.of()));
        send(token, sessionId, "다음 근무 때 주문 나갈 때 영수증이랑 한 번 더 대조해 볼래")
                .andExpect(jsonPath("$.data.currentStep").value("STEP_05"))
                .andExpect(jsonPath("$.data.nextInput").value("POST_INTENSITY"));

        // 상담 후 강도 → STEP_06 정리, 결과 저장, 말랑이 해금
        ai.push(reply("오늘 이야기를 정리해 보았소.\n처음엔 일 못하는 사람인가 싶었지만 손님이 몰린 날이었다는 것도 찾았소.", true, null, Map.of(
                "initialThought", "나는 일 못하는 사람인가 봐요.",
                "alternativeThought", "그날 손님이 몰렸고 실수 하나로 일 못하는 사람은 아닌 것 같아요.",
                "actionPlan", "다음 근무 때 영수증과 한 번 더 대조하기",
                "summary", "알바 중 손님 앞에서 지적받아 속상했어요",
                "keySentence", "“일 못하는 사람인가 봐”보다 “손님이 몰린 날이었다”가 지금은 더 가까운 말 같아요",
                "malangiReason", "속상했던 마음을 부드럽게 감싸 쥐며 달래 봐요."), List.of()));
        call(token, "/" + sessionId + "/complete", "{\"postIntensity\":4}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.preIntensity").value(7))
                .andExpect(jsonPath("$.data.postIntensity").value(4))
                .andExpect(jsonPath("$.data.intensityChange").value(-3))
                .andExpect(jsonPath("$.data.summary").value("알바 중 손님 앞에서 지적받아 속상했어요"))
                .andExpect(jsonPath("$.data.record.initialThought").value("나는 일 못하는 사람인가 봐요."))
                .andExpect(jsonPath("$.data.recommendedMalangi.type").value("SQUISHY"))
                .andExpect(jsonPath("$.data.recommendedMalangi.isNewlyUnlocked").value(true));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM counseling_results WHERE session_id = ?",
                Integer.class, sessionId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT is_unlocked FROM user_malangis um JOIN users u ON u.id = um.user_id "
                + "WHERE u.email = ? AND um.malangi_type = 'SQUISHY'", Boolean.class, "counsel-flow@example.com")).isTrue();
        // 끝난 세션에는 더 보낼 수 없다
        send(token, sessionId, "하나 더").andExpect(status().isConflict());
    }

    @Test
    void stopsImmediatelyOnFirstLineCrisisSignal() throws Exception {
        String token = registerAndLogin("counsel-risk1@example.com");
        start(token, "요즘은 그냥 죽고 싶다는 생각만 들어")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("ABORTED_RISK"))
                .andExpect(jsonPath("$.data.riskDetected").value(true))
                .andExpect(jsonPath("$.data.emergencyHotlines[0].tel").value("109"))
                .andExpect(jsonPath("$.data.nextInput").value("NONE"));
        assertThat(ai.requests()).isEmpty();
    }

    @Test
    void stopsWhenSafetyClassifierFlagsRisk() throws Exception {
        String token = registerAndLogin("counsel-risk2@example.com");
        ai.risky(true);
        ai.push(reply("많이 지쳤구려.\n어떤 일이 있었는지 들려주겠소?", false, "situation", Map.of(), List.of()));
        start(token, "요즘은 다 끝내 버리고 싶어")
                .andExpect(jsonPath("$.data.status").value("ABORTED_RISK"))
                .andExpect(jsonPath("$.data.riskDetected").value(true));
    }

    @Test
    void retriesOnceWhenReplyBreaksRules() throws Exception {
        String token = registerAndLogin("counsel-retry@example.com");
        // 해요체 → 서버가 규칙 위반으로 한 번 다시 요청
        ai.push(reply("많이 속상했겠어요.\n무슨 일이 있었나요?", false, "situation", Map.of(), List.of()));
        ai.push(reply("많이 속상했을 것 같소.\n어떤 일이 있었는지 조금 더 들려주겠소?", false, "situation", Map.of(), List.of()));
        start(token, "알바하다가 또 실수했어")
                .andExpect(jsonPath("$.data.aiMessage").value("많이 속상했을 것 같소.\n어떤 일이 있었는지 조금 더 들려주겠소?"))
                .andExpect(jsonPath("$.data.nextInput").value("MESSAGE"));
        assertThat(ai.requests()).hasSize(2);
        assertThat(ai.requests().get(1).userText()).contains("서버 지시: 이전 응답이 규칙에 맞지 않았다");
    }

    @Test
    void answersWithFallbackWhenAiIsUnavailable() throws Exception {
        String token = registerAndLogin("counsel-fallback@example.com");
        start(token, "오늘 너무 힘들었어")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.data.aiMessage").value("잠시 답을 준비하지 못했소.\n어떤 일이 있었는지 한 번만 더 들려주겠소?"));
    }

    @Test
    void rejectsInputThatTheFlowDoesNotExpect() throws Exception {
        String token = registerAndLogin("counsel-order@example.com");
        ai.push(reply("발표를 망쳐 속상했구려.\n그때의 감정을 먼저 같이 정리해 보면 좋을 것 같소.", true, null,
                Map.of("situation", "발표 중 준비한 말을 까먹음"), List.of()));
        long sessionId = sessionId(start(token, "동아리 발표에서 준비한 말을 까먹고 버벅였어"));

        send(token, sessionId, "카드 대신 그냥 말할래")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("INPUT_NOT_EXPECTED"));
        call(token, "/" + sessionId + "/complete", "{\"postIntensity\":3}").andExpect(status().isConflict());
        call(token, "/" + sessionId + "/emotion", "{\"primaryEmotion\":\"속상함\",\"preIntensity\":11}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void abortsAndDeletesOnlyOwnSessions() throws Exception {
        String owner = registerAndLogin("counsel-owner@example.com");
        String other = registerAndLogin("counsel-other@example.com");
        ai.push(reply("많이 지쳤구려.\n어떤 일이 있었는지 들려주겠소?", false, "situation", Map.of(), List.of()));
        long sessionId = sessionId(start(owner, "그냥 다 귀찮아"));

        call(other, "/" + sessionId + "/abort", "{}").andExpect(status().isNotFound());
        call(owner, "/" + sessionId + "/abort", "{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABORTED_USER"));
        mockMvc.perform(delete("/api/v1/counseling/sessions/" + sessionId).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/counseling/sessions/" + sessionId).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM counseling_messages WHERE session_id = ?",
                Integer.class, sessionId)).isZero();
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/counseling/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"안녕\"}"))
                .andExpect(status().isUnauthorized());
    }

    // ---------- helpers ----------

    private ResultActions start(String token, String message) throws Exception {
        return mockMvc.perform(post("/api/v1/counseling/sessions")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\":" + quote(message) + "}"));
    }

    private ResultActions send(String token, long sessionId, String message) throws Exception {
        return call(token, "/" + sessionId + "/messages", "{\"message\":" + quote(message) + "}");
    }

    private ResultActions call(String token, String path, String body) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/counseling/sessions" + path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static long sessionId(ResultActions actions) throws Exception {
        return ((Number) JsonPath.read(actions.andReturn().getResponse().getContentAsString(), "$.data.sessionId"))
                .longValue();
    }

    private static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String registerAndLogin(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!","nickname":"말랑친구"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
    }
}
