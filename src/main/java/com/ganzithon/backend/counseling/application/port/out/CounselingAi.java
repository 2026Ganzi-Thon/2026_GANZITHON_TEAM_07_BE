package com.ganzithon.backend.counseling.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * LLM 호출 (02번 이슈: Spring AI, 05번 이슈: 구조화 출력).
 * 프롬프트 조립·검증·재시도·타임아웃은 애플리케이션이 하고, 어댑터는 호출과 JSON 해석만 한다.
 */
public interface CounselingAi {
    ConversationReply converse(ConversationRequest request);

    SafetyVerdict classify(String systemPrompt, String userContent);

    /** 월간 분석 레포트 (F-12, 테스형의 분석 레포트). */
    ReportReply monthlyReport(String systemPrompt, String userContent);

    /**
     * @param cachedSystem 공통 프롬프트 + 단계 프롬프트 (세션 간에 같아 캐시한다)
     * @param stateSystem 현재 상태 블록 (매 턴 바뀐다)
     */
    record ConversationRequest(String cachedSystem, String stateSystem, List<ChatTurn> history, String userText,
            boolean summary) {}

    record ChatTurn(boolean fromUser, String text) {}

    /**
     * @param extracted 이번 응답에서 값이 온 슬롯만 담는다 (null = 변화 없음은 키가 없다, "" = 답 없음)
     */
    record ConversationReply(String userMessage, boolean transitionReady, String questionTarget,
            Map<String, String> extracted, List<String> replyOptions, boolean endRequested, String backToStep) {}

    record SafetyVerdict(boolean riskFlag, String category) {}

    record ReportReply(String summary, List<String> thoughtPatterns, List<String> helpfulCoping, String changedView,
            String suggestion) {}

    /** 호출 실패(네트워크, 형식 오류, 키 없음 등). 애플리케이션이 재시도·폴백한다. */
    class Unavailable extends RuntimeException {
        public Unavailable(String message, Throwable cause) {
            super(message, cause);
        }

        public Unavailable(String message) {
            super(message);
        }
    }
}
