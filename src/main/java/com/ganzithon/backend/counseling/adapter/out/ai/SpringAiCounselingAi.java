package com.ganzithon.backend.counseling.adapter.out.ai;

import com.ganzithon.backend.counseling.application.port.out.CounselingAi;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.anthropic.AnthropicCacheOptions;
import org.springframework.ai.anthropic.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Claude 호출 어댑터 (02번 이슈: Spring AI). 구조화 출력(JSON Schema)과 시스템 프롬프트 캐싱을 쓴다.
 * 시스템 프롬프트는 [공통 + 단계](캐시)와 [현재 상태](매 턴 바뀜) 두 블록으로 보낸다.
 * LLM_API_KEY가 없으면 호출 때 Unavailable을 던지고, 서비스가 기본 문장으로 응답한다.
 */
@Component
class SpringAiCounselingAi implements CounselingAi {
    private static final String CONVERSATION_SCHEMA = "prompts/emotion-conversation/output.schema.json";
    private static final String SAFETY_SCHEMA = "prompts/safety-classifier/output.schema.json";
    private static final String REPORT_SCHEMA = "prompts/monthly-report/output.schema.json";

    private final JsonMapper json = JsonMapper.builder().build();
    private final ChatModel chatModel;
    private final String model;
    private final int maxTokens;
    private final String conversationSchema;
    private final String safetySchema;
    private final String reportSchema;

    SpringAiCounselingAi(@Value("${counseling.ai.api-key:}") String apiKey,
            @Value("${counseling.ai.model:claude-haiku-5-5}") String model,
            @Value("${counseling.ai.max-tokens:2048}") int maxTokens) {
        this.model = model;
        this.maxTokens = maxTokens;
        this.conversationSchema = claudeSchema(read(CONVERSATION_SCHEMA));
        this.safetySchema = claudeSchema(read(SAFETY_SCHEMA));
        this.reportSchema = claudeSchema(read(REPORT_SCHEMA));
        this.chatModel = apiKey == null || apiKey.isBlank() ? null : AnthropicChatModel.builder()
                .options(AnthropicChatOptions.builder()
                        .apiKey(apiKey)
                        .model(model)
                        .maxTokens(maxTokens)
                        .timeout(Duration.ofSeconds(30)) // 호출별 타임아웃은 서비스가 따로 건다
                        .maxRetries(0) // 재시도는 서비스가 1회 한다 (07번 이슈)
                        .build())
                .build();
    }

    @Override
    public ConversationReply converse(ConversationRequest request) {
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(request.cachedSystem()));
        messages.add(new SystemMessage(request.stateSystem()));
        for (ChatTurn turn : request.history()) {
            messages.add(turn.fromUser() ? new UserMessage(turn.text()) : new AssistantMessage(turn.text()));
        }
        messages.add(new UserMessage(request.userText()));
        Map<String, Object> data = call(messages, conversationSchema, true);

        Map<String, String> extracted = new LinkedHashMap<>();
        if (data.get("extractedData") instanceof Map<?, ?> slots) {
            slots.forEach((key, value) -> {
                if (value != null) extracted.put(String.valueOf(key), String.valueOf(value));
            });
        }
        List<String> options = new ArrayList<>();
        if (data.get("replyOptions") instanceof List<?> list) {
            list.forEach(item -> {
                if (item != null && !String.valueOf(item).isBlank()) options.add(String.valueOf(item).strip());
            });
        }
        return new ConversationReply(text(data.get("userMessage")), Boolean.TRUE.equals(data.get("transitionReady")),
                text(data.get("questionTarget")), extracted, options, Boolean.TRUE.equals(data.get("endRequested")),
                text(data.get("backToStep")));
    }

    @Override
    public SafetyVerdict classify(String systemPrompt, String userContent) {
        Map<String, Object> data = call(List.of(new SystemMessage(systemPrompt), new UserMessage(userContent)),
                safetySchema, false);
        if (!(data.get("riskFlag") instanceof Boolean riskFlag)) throw new Unavailable("riskFlag가 없습니다");
        return new SafetyVerdict(riskFlag, text(data.get("category")));
    }

    @Override
    public ReportReply monthlyReport(String systemPrompt, String userContent) {
        Map<String, Object> data = call(List.of(new SystemMessage(systemPrompt), new UserMessage(userContent)),
                reportSchema, false);
        return new ReportReply(text(data.get("summary")), strings(data.get("thoughtPatterns")),
                strings(data.get("helpfulCoping")), text(data.get("changedView")), text(data.get("suggestion")));
    }

    private static List<String> strings(Object value) {
        List<String> out = new ArrayList<>();
        if (value instanceof List<?> list) {
            list.forEach(item -> {
                if (item != null && !String.valueOf(item).isBlank()) out.add(String.valueOf(item).strip());
            });
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> call(List<Message> messages, String schema, boolean splitSystem) {
        if (chatModel == null) throw new Unavailable("LLM_API_KEY가 설정되지 않았습니다");
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(model)
                .maxTokens(maxTokens)
                .outputSchema(schema)
                .cacheOptions(AnthropicCacheOptions.builder()
                        .strategy(AnthropicCacheStrategy.SYSTEM_ONLY)
                        .multiBlockSystemCaching(splitSystem)
                        .build())
                .build();
        String text;
        try {
            ChatResponse response = chatModel.call(new Prompt(messages, options));
            text = response == null || response.getResult() == null ? null : response.getResult().getOutput().getText();
        } catch (RuntimeException exception) {
            throw new Unavailable("LLM 호출 실패: " + exception.getMessage(), exception);
        }
        if (text == null || text.isBlank()) throw new Unavailable("LLM 응답이 비었습니다");
        try {
            return json.readValue(stripFence(text), Map.class);
        } catch (RuntimeException exception) {
            throw new Unavailable("LLM 응답 JSON 해석 실패", exception);
        }
    }

    /** output.schema.json을 Claude 구조화 출력이 받는 모양으로 바꾼다 (eval/llm.py claude_schema와 같은 규칙). */
    @SuppressWarnings("unchecked")
    private String claudeSchema(String raw) {
        Map<String, Object> schema = (Map<String, Object>) walk(json.readValue(raw, Map.class));
        if (schema.get("properties") instanceof Map<?, ?> properties) {
            schema.put("required", new ArrayList<Object>(properties.keySet()));
        }
        return json.writeValueAsString(schema);
    }

    private static Object walk(Object node) {
        if (node instanceof Map<?, ?> map) {
            Map<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                String k = String.valueOf(key);
                if (!List.of("$schema", "title", "minLength", "maxLength", "minItems", "maxItems", "minimum", "maximum")
                        .contains(k)) {
                    out.put(k.equals("oneOf") ? "anyOf" : k, walk(value));
                }
            });
            if ("object".equals(out.get("type"))) out.put("additionalProperties", false);
            return out;
        }
        if (node instanceof List<?> list) {
            List<Object> out = new ArrayList<>();
            list.forEach(item -> out.add(walk(item)));
            return out;
        }
        return node;
    }

    private static String stripFence(String text) {
        String t = text.strip();
        if (t.startsWith("```")) {
            int start = t.indexOf('\n');
            int end = t.lastIndexOf("```");
            if (start > 0 && end > start) t = t.substring(start + 1, end).strip();
        }
        return t;
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String read(String path) {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("프롬프트 스키마를 읽지 못했습니다: " + path, exception);
        }
    }
}
