package com.ganzithon.backend.auth.adapter.in.web;

import com.ganzithon.backend.auth.application.port.in.AuthUseCase;
import com.ganzithon.backend.auth.application.port.in.AuthUseCase.AuthenticatedUser;
import com.ganzithon.backend.auth.application.port.in.AuthUseCase.LoginCommand;
import com.ganzithon.backend.auth.application.port.in.AuthUseCase.SignupCommand;
import com.ganzithon.backend.auth.application.port.in.AuthUseCase.SocialLoginCommand;
import com.ganzithon.backend.auth.domain.AuthProvider;
import com.ganzithon.backend.auth.domain.AuthUser;
import com.ganzithon.backend.auth.domain.TokenPair;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthUseCase auth;

    public AuthController(AuthUseCase auth) {
        this.auth = auth;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiEnvelope<ProfileData>> signup(@Valid @RequestBody SignupRequest request) {
        AuthUser user = auth.signup(new SignupCommand(request.email(), request.password(), request.nickname()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiEnvelope.success(ProfileData.from(user)));
    }

    @PostMapping("/login")
    public ApiEnvelope<TokenData> login(@Valid @RequestBody LoginRequest request) {
        return ApiEnvelope.success(TokenData.from(auth.login(new LoginCommand(request.email(), request.password()))));
    }

    @PostMapping("/oauth/{provider}")
    public ApiEnvelope<SocialTokenData> socialLogin(
            @PathVariable String provider,
            @Valid @RequestBody SocialLoginRequest request
    ) {
        AuthProvider authProvider;
        try {
            authProvider = AuthProvider.valueOf(provider.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("지원하지 않는 소셜 로그인 제공자입니다.");
        }
        AuthenticatedUser authenticated = auth.socialLogin(new SocialLoginCommand(
                authProvider, request.authorizationCode(), request.redirectUri()));
        return ApiEnvelope.success(SocialTokenData.from(authenticated));
    }

    @PostMapping("/refresh")
    public ApiEnvelope<RefreshTokenData> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiEnvelope.success(RefreshTokenData.from(auth.refresh(request.refreshToken())));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody LogoutRequest request
    ) {
        auth.logout(Long.parseLong(jwt.getSubject()), request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ApiEnvelope<ProfileData> profile(@AuthenticationPrincipal Jwt jwt) {
        return ApiEnvelope.success(ProfileData.from(auth.profile(Long.parseLong(jwt.getSubject()))));
    }

    public record SignupRequest(
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72) String password,
            @NotBlank @Size(max = 50) String nickname
    ) {}

    public record LoginRequest(@NotBlank @Email @Size(max = 255) String email, @NotBlank String password) {}

    public record SocialLoginRequest(@NotBlank String authorizationCode, @NotBlank String redirectUri) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record LogoutRequest(@NotBlank String refreshToken) {}

    public record ProfileData(long userId, String email, String nickname, String provider) {
        static ProfileData from(AuthUser user) {
            return new ProfileData(user.id(), user.email(), user.nickname(), user.provider().name());
        }
    }

    public record TokenData(String accessToken, String refreshToken, String tokenType, long expiresIn,
                            long userId, String nickname) {
        static TokenData from(AuthenticatedUser authenticated) {
            return new TokenData(authenticated.tokens().accessToken(), authenticated.tokens().refreshToken(),
                    "Bearer", authenticated.tokens().expiresIn(), authenticated.user().id(), authenticated.user().nickname());
        }
    }

    public record SocialTokenData(String accessToken, String refreshToken, String tokenType, long expiresIn,
                                  long userId, String email, String nickname, String provider, boolean isNewUser) {
        static SocialTokenData from(AuthenticatedUser authenticated) {
            return new SocialTokenData(authenticated.tokens().accessToken(), authenticated.tokens().refreshToken(),
                    "Bearer", authenticated.tokens().expiresIn(), authenticated.user().id(), authenticated.user().email(),
                    authenticated.user().nickname(), authenticated.user().provider().name(), authenticated.newUser());
        }
    }

    public record RefreshTokenData(String accessToken, String refreshToken, String tokenType, long expiresIn) {
        static RefreshTokenData from(TokenPair pair) {
            return new RefreshTokenData(pair.accessToken(), pair.refreshToken(), "Bearer", pair.expiresIn());
        }
    }
}
