package com.ganzithon.backend.auth.adapter.in.web;

import com.jayway.jsonpath.JsonPath;
import com.ganzithon.backend.auth.application.port.out.SocialProfileClient;
import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.SocialProfile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTests {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    SocialProfileClient socialProfileClient;

    @Test
    void signsUpLogsInAndAuthenticatesTheProfileRequest() throws Exception {
        String email = "auth-flow@example.com";
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!","nickname":"슬랑이친구"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.nickname").value("슬랑이친구"));

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andReturn();

        String accessToken = JsonPath.read(login.getResponse().getContentAsString(), "$.data.accessToken");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value(email));
    }

    @Test
    void rejectsDuplicateEmailRegistration() throws Exception {
        String body = """
                {"email":"duplicate@example.com","password":"Password123!","nickname":"말랑이친구"}
                """;
        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    @Test
    void rotatesRefreshTokenAndRejectsThePreviousToken() throws Exception {
        register("rotation@example.com");
        String initialRefreshToken = login("rotation@example.com");

        MvcResult refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(initialRefreshToken)))
                .andExpect(status().isOk())
                .andReturn();
        String rotatedRefreshToken = JsonPath.read(refreshed.getResponse().getContentAsString(), "$.data.refreshToken");
        assertThat(rotatedRefreshToken).isNotEqualTo(initialRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(initialRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesTheRefreshToken() throws Exception {
        register("logout@example.com");
        String loginResponse = loginResponse("logout@example.com");
        String refreshToken = JsonPath.read(loginResponse, "$.data.refreshToken");
        String accessToken = JsonPath.read(loginResponse, "$.data.accessToken");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAuthenticationForProfileAndRejectsRefreshTokenAsAccessToken() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
        register("wrong-token-type@example.com");
        String refreshToken = login("wrong-token-type@example.com");
        mockMvc.perform(get("/api/v1/auth/me").header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsSocialAccountsAndRejectsCrossProviderEmailReuse() throws Exception {
        when(socialProfileClient.exchange(eq(AuthProvider.KAKAO), anyString(), anyString()))
                .thenReturn(new SocialProfile("kakao-42", "social@example.com", "카카오친구", true));
        MvcResult firstLogin = mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationCode\":\"code-1\",\"redirectUri\":\"https://app.example/kakao\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("KAKAO"))
                .andExpect(jsonPath("$.data.isNewUser").value(true))
                .andReturn();

        MvcResult returningLogin = mockMvc.perform(post("/api/v1/auth/oauth/kakao")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationCode\":\"code-2\",\"redirectUri\":\"https://app.example/kakao\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isNewUser").value(false))
                .andReturn();
        Integer returningUserId = JsonPath.read(returningLogin.getResponse().getContentAsString(), "$.data.userId");
        Integer firstUserId = JsonPath.read(firstLogin.getResponse().getContentAsString(), "$.data.userId");
        assertThat(returningUserId).isEqualTo(firstUserId);

        register("collision@example.com");
        when(socialProfileClient.exchange(eq(AuthProvider.DISCORD), anyString(), anyString()))
                .thenReturn(new SocialProfile("discord-42", "collision@example.com", "Discord친구", true));
        mockMvc.perform(post("/api/v1/auth/oauth/discord")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"authorizationCode\":\"code-3\",\"redirectUri\":\"https://app.example/discord\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.data.existingProvider").value("LOCAL"))
                .andExpect(jsonPath("$.data.registeredEmail").value("collision@example.com"));
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Password123!","nickname":"테스트친구"}
                                """.formatted(email)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        return JsonPath.read(loginResponse(email), "$.data.refreshToken");
    }

    private String loginResponse(String email) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"Password123!\"}".formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
