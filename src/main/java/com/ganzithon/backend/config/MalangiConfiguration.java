package com.ganzithon.backend.config;

import com.ganzithon.backend.malangi.application.port.in.MalangiUseCase;
import com.ganzithon.backend.malangi.application.port.out.MalangiInventoryStore;
import com.ganzithon.backend.malangi.application.port.out.MalangiSessionStore;
import com.ganzithon.backend.malangi.application.service.MalangiService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MalangiConfiguration {
    @Bean
    MalangiUseCase malangiUseCase(MalangiInventoryStore inventory, MalangiSessionStore sessions, Clock authClock) {
        return new MalangiService(inventory, sessions, authClock);
    }
}
