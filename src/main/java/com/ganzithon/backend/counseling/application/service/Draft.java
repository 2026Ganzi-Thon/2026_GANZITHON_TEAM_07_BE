package com.ganzithon.backend.counseling.application.service;

import com.ganzithon.backend.counseling.application.port.out.CounselingAi.ConversationReply;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 서버가 검증·보정하는 동안 다루는 AI 응답 사본. */
final class Draft {
    String userMessage;
    boolean transitionReady;
    String questionTarget;
    final Map<String, String> extracted;
    List<String> replyOptions;
    boolean endRequested;
    String backToStep;

    /** 단계 필수 항목이 다 찼는데 질문을 덧붙여 서버가 단계 완료로 처리함 */
    boolean serverFinished;
    /** 허용되지 않은 단계의 replyOptions를 서버가 버림 */
    boolean optionsDropped;

    Draft(ConversationReply reply) {
        this.userMessage = reply.userMessage() == null ? "" : reply.userMessage();
        this.transitionReady = reply.transitionReady();
        this.questionTarget = blankToNull(reply.questionTarget());
        this.extracted = new LinkedHashMap<>(reply.extracted() == null ? Map.of() : reply.extracted());
        this.replyOptions = new ArrayList<>(reply.replyOptions() == null ? List.of() : reply.replyOptions());
        this.endRequested = reply.endRequested();
        this.backToStep = blankToNull(reply.backToStep());
    }

    boolean asks() {
        return userMessage.contains("?");
    }

    String extracted(String key) {
        return extracted.get(key);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
