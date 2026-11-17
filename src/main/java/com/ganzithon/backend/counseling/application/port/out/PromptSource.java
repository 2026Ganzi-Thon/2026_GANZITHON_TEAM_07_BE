package com.ganzithon.backend.counseling.application.port.out;

import com.ganzithon.backend.counseling.domain.CounselingStep;

/** src/main/resources/prompts 아래 프롬프트 원문. */
public interface PromptSource {
    String common();

    String step(CounselingStep step);

    String safety();
}
