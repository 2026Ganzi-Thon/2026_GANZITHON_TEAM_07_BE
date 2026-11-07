package com.ganzithon.backend.auth.adapter.out.oauth;

import com.ganzithon.backend.auth.application.port.out.SocialProfileClient;
import com.ganzithon.backend.auth.application.port.in.AuthFailure;
import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.SocialProfile;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
class OAuthSocialProfileAdapter implements SocialProfileClient {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_TYPE = new ParameterizedTypeReference<>() {};
    private final RestClient restClient;
    private final Map<AuthProvider, Registration> registrations;

    OAuthSocialProfileAdapter(
            RestClient.Builder restClientBuilder,
            @Value("${auth.oauth.kakao.client-id:}") String kakaoClientId,
            @Value("${auth.oauth.kakao.client-secret:}") String kakaoClientSecret,
            @Value("${auth.oauth.kakao.redirect-uri:}") String kakaoRedirectUri,
            @Value("${auth.oauth.discord.client-id:}") String discordClientId,
            @Value("${auth.oauth.discord.client-secret:}") String discordClientSecret,
            @Value("${auth.oauth.discord.redirect-uri:}") String discordRedirectUri,
            @Value("${auth.oauth.google.client-id:}") String googleClientId,
            @Value("${auth.oauth.google.client-secret:}") String googleClientSecret,
            @Value("${auth.oauth.google.redirect-uri:}") String googleRedirectUri
    ) {
        this.restClient = restClientBuilder.build();
        this.registrations = Map.of(
                AuthProvider.KAKAO, new Registration(kakaoClientId, kakaoClientSecret, kakaoRedirectUri,
                        "https://kauth.kakao.com/oauth/token", "https://kapi.kakao.com/v2/user/me"),
                AuthProvider.DISCORD, new Registration(discordClientId, discordClientSecret, discordRedirectUri,
                        "https://discord.com/api/oauth2/token", "https://discord.com/api/users/@me"),
                AuthProvider.GOOGLE, new Registration(googleClientId, googleClientSecret, googleRedirectUri,
                        "https://oauth2.googleapis.com/token", "https://openidconnect.googleapis.com/v1/userinfo"));
    }

    @Override
    public SocialProfile exchange(AuthProvider provider, String authorizationCode, String redirectUri) {
        Registration registration = registrations.get(provider);
        if (registration == null) {
            throw new AuthFailure(AuthFailure.Code.INVALID_REQUEST);
        }
        if (isBlank(registration.clientId()) || isBlank(registration.clientSecret())
                || isBlank(registration.redirectUri())) {
            throw new AuthFailure(AuthFailure.Code.SOCIAL_PROVIDER_UNAVAILABLE);
        }
        if (!registration.redirectUri().equals(redirectUri)) {
            throw new AuthFailure(AuthFailure.Code.INVALID_SOCIAL_CREDENTIALS);
        }

        try {
            String accessToken = exchangeCode(registration, authorizationCode, redirectUri);
            Map<String, Object> profile = restClient.get()
                    .uri(registration.profileUri())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(MAP_TYPE);
            return toProfile(provider, profile);
        } catch (RestClientResponseException exception) {
            HttpStatusCode status = exception.getStatusCode();
            if (status.is4xxClientError()) {
                throw new AuthFailure(AuthFailure.Code.INVALID_SOCIAL_CREDENTIALS);
            }
            throw new AuthFailure(AuthFailure.Code.SOCIAL_PROVIDER_UNAVAILABLE);
        } catch (RestClientException exception) {
            throw new AuthFailure(AuthFailure.Code.SOCIAL_PROVIDER_UNAVAILABLE);
        }
    }

    private String exchangeCode(Registration registration, String authorizationCode, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", registration.clientId());
        form.add("client_secret", registration.clientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("code", authorizationCode);
        Map<String, Object> response = restClient.post()
                .uri(registration.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(MAP_TYPE);
        String accessToken = stringValue(response, "access_token");
        if (isBlank(accessToken)) {
            throw new AuthFailure(AuthFailure.Code.INVALID_SOCIAL_CREDENTIALS);
        }
        return accessToken;
    }

    private SocialProfile toProfile(AuthProvider provider, Map<String, Object> response) {
        if (response == null) {
            throw new AuthFailure(AuthFailure.Code.INVALID_SOCIAL_CREDENTIALS);
        }
        return switch (provider) {
            case KAKAO -> kakaoProfile(response);
            case DISCORD -> discordProfile(response);
            case GOOGLE -> googleProfile(response);
            case LOCAL -> throw new AuthFailure(AuthFailure.Code.INVALID_REQUEST);
        };
    }

    private SocialProfile kakaoProfile(Map<String, Object> response) {
        Map<String, Object> account = mapValue(response, "kakao_account");
        Map<String, Object> properties = mapValue(response, "properties");
        Map<String, Object> profile = mapValue(account, "profile");
        String nickname = firstNonBlank(stringValue(properties, "nickname"), stringValue(profile, "nickname"));
        Object emailVerified = account.get("is_email_verified");
        if (emailVerified == null) {
            emailVerified = account.get("is_email_valid");
        }
        return new SocialProfile(stringValue(response, "id"), stringValue(account, "email"), nickname,
                Boolean.TRUE.equals(emailVerified));
    }

    private SocialProfile discordProfile(Map<String, Object> response) {
        String nickname = firstNonBlank(stringValue(response, "global_name"), stringValue(response, "username"));
        return new SocialProfile(stringValue(response, "id"), stringValue(response, "email"), nickname,
                Boolean.TRUE.equals(response.get("verified")));
    }

    private SocialProfile googleProfile(Map<String, Object> response) {
        return new SocialProfile(stringValue(response, "sub"), stringValue(response, "email"),
                stringValue(response, "name"), Boolean.TRUE.equals(response.get("email_verified")));
    }

    private static Map<String, Object> mapValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        map.forEach((mapKey, mapValue) -> result.put(String.valueOf(mapKey), mapValue));
        return result;
    }

    private static String stringValue(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Registration(String clientId, String clientSecret, String redirectUri,
                                String tokenUri, String profileUri) {}
}
