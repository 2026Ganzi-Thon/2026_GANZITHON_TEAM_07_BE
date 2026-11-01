package com.ganzithon.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("GANZITHON TEAM 07 API")
                .description("감정 탐색 대화를 위한 백엔드 API")
                .version("v1"));
    }
}
