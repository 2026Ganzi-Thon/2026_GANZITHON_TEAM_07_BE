package com.ganzithon.backend.config;

import com.ganzithon.backend.health.application.port.in.CheckHealthUseCase;
import com.ganzithon.backend.health.application.service.HealthService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HealthConfig {

    @Bean
    public CheckHealthUseCase checkHealthUseCase() {
        return new HealthService();
    }
}
