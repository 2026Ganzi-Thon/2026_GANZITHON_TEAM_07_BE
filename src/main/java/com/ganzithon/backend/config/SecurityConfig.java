package com.ganzithon.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.beans.factory.annotation.Qualifier;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, @Qualifier("jwtDecoder") JwtDecoder accessTokenDecoder)
            throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/v1/health", "/actuator/health", "/v3/api-docs/**", "/swagger-ui/**")
                        .permitAll()
                        .requestMatchers("/api/v1/auth/signup", "/api/v1/auth/login", "/api/v1/auth/oauth/**",
                                "/api/v1/auth/refresh")
                        .permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(accessTokenDecoder)))
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> writeFailure(
                                response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."))
                        .accessDeniedHandler((request, response, exception) -> writeFailure(
                                response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다.")))
                .build();
    }

    private static void writeFailure(HttpServletResponse response, int status, String code, String message)
            throws IOException {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                + code + "\",\"message\":\"" + message + "\"}}");
    }
}
