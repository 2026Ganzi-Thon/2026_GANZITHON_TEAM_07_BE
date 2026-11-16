package com.ganzithon.backend.config;

import com.ganzithon.backend.counseling.application.port.in.CounselingUseCase;
import com.ganzithon.backend.counseling.application.port.out.CounselingAi;
import com.ganzithon.backend.counseling.application.port.out.CounselingSessionStore;
import com.ganzithon.backend.counseling.application.port.out.MalangiReward;
import com.ganzithon.backend.counseling.application.port.out.PromptSource;
import com.ganzithon.backend.counseling.application.service.CounselingService;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class CounselingConfiguration {
    @Bean
    CounselingUseCase counselingUseCase(CounselingSessionStore store, CounselingAi ai, MalangiReward reward,
            PromptSource prompts, Clock authClock,
            @Value("${counseling.ai.timeout.conversation:8s}") Duration conversationTimeout,
            @Value("${counseling.ai.timeout.summary:15s}") Duration summaryTimeout,
            @Value("${counseling.ai.timeout.safety:5s}") Duration safetyTimeout) {
        return new CounselingService(store, ai, reward, prompts, authClock, Executors.newVirtualThreadPerTaskExecutor(),
                new CounselingService.Timeouts(conversationTimeout, summaryTimeout, safetyTimeout));
    }
}
