package com.ganzithon.backend.auth.adapter.out.oauth;

import com.ganzithon.backend.auth.application.port.in.AuthFailure;
import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.SocialProfile;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OAuthSocialProfileAdapterTests {

    @Test
    void exchangesKakaoAuthorizationCodeAndReadsVerifiedProfile() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OAuthSocialProfileAdapter adapter = adapter(builder);
        server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("client_id=kakao-client")))
                .andExpect(content().string(containsString("code=auth-code")))
                .andRespond(withSuccess("{\"access_token\":\"kakao-access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":42,"properties":{"nickname":"카카오친구"},"kakao_account":{"email":"kakao@example.com","is_email_verified":true}}
                        """, MediaType.APPLICATION_JSON));

        SocialProfile profile = adapter.exchange(AuthProvider.KAKAO, "auth-code", "https://app.example/kakao");

        assertThat(profile).isEqualTo(new SocialProfile("42", "kakao@example.com", "카카오친구", true));
        server.verify();
    }

    @Test
    void exchangesDiscordAuthorizationCodeAndReadsVerifiedProfile() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OAuthSocialProfileAdapter adapter = adapter(builder);
        server.expect(requestTo("https://discord.com/api/oauth2/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("client_id=discord-client")))
                .andRespond(withSuccess("{\"access_token\":\"discord-access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://discord.com/api/users/@me"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"id":"discord-7","global_name":"Discord친구","username":"user","email":"discord@example.com","verified":true}
                        """, MediaType.APPLICATION_JSON));

        SocialProfile profile = adapter.exchange(AuthProvider.DISCORD, "auth-code", "https://app.example/discord");

        assertThat(profile).isEqualTo(new SocialProfile("discord-7", "discord@example.com", "Discord친구", true));
        server.verify();
    }

    @Test
    void exchangesGoogleAuthorizationCodeAndReadsVerifiedProfile() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OAuthSocialProfileAdapter adapter = adapter(builder);
        server.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("client_id=google-client")))
                .andRespond(withSuccess("{\"access_token\":\"google-access\"}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://openidconnect.googleapis.com/v1/userinfo"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {"sub":"google-8","email":"google@example.com","email_verified":true,"name":"Google친구"}
                        """, MediaType.APPLICATION_JSON));

        SocialProfile profile = adapter.exchange(AuthProvider.GOOGLE, "auth-code", "https://app.example/google");

        assertThat(profile).isEqualTo(new SocialProfile("google-8", "google@example.com", "Google친구", true));
        server.verify();
    }

    @Test
    void rejectsUnregisteredRedirectUriBeforeContactingProvider() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        OAuthSocialProfileAdapter adapter = adapter(builder);

        assertThatThrownBy(() -> adapter.exchange(AuthProvider.KAKAO, "auth-code", "https://attacker.example/callback"))
                .isInstanceOf(AuthFailure.class)
                .extracting(exception -> ((AuthFailure) exception).code())
                .isEqualTo(AuthFailure.Code.INVALID_SOCIAL_CREDENTIALS);
        server.verify();
    }

    private static OAuthSocialProfileAdapter adapter(RestClient.Builder builder) {
        return new OAuthSocialProfileAdapter(builder,
                "kakao-client", "kakao-secret", "https://app.example/kakao",
                "discord-client", "discord-secret", "https://app.example/discord",
                "google-client", "google-secret", "https://app.example/google");
    }
}
