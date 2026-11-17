package com.ganzithon.backend.counseling.adapter.out.ai;

import com.ganzithon.backend.counseling.application.port.out.PromptSource;
import com.ganzithon.backend.counseling.domain.CounselingStep;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** src/main/resources/prompts 아래 프롬프트 파일을 읽는다. */
@Component
class ClasspathPromptSource implements PromptSource {
    private static final String BASE = "prompts/";

    @Override
    public String common() {
        return read("emotion-conversation/common.md");
    }

    @Override
    public String step(CounselingStep step) {
        return read("emotion-conversation/steps/" + step.name() + ".md");
    }

    @Override
    public String safety() {
        return read("safety-classifier/system.md");
    }

    private static String read(String path) {
        try (InputStream in = new ClassPathResource(BASE + path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("프롬프트를 읽지 못했습니다: " + path, exception);
        }
    }
}
