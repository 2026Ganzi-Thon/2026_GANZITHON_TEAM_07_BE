package com.ganzithon.backend.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.ganzithon.backend.auth.application.port.in.AuthUseCase;
import com.ganzithon.backend.auth.application.port.out.AuthTokenIssuer;
import com.ganzithon.backend.auth.application.port.out.AuthTransaction;
import com.ganzithon.backend.auth.application.port.out.AuthUserStore;
import com.ganzithon.backend.auth.application.port.out.PasswordHasher;
import com.ganzithon.backend.auth.application.port.out.RefreshTokenStore;
import com.ganzithon.backend.auth.application.port.out.SocialProfileClient;
import com.ganzithon.backend.malangi.application.port.out.MalangiInventoryStore;
import com.ganzithon.backend.auth.application.service.AuthService;
import java.time.Clock;
import java.util.Base64;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
public class AuthConfiguration {

    @Bean
    SecretKey authJwtSecretKey(@Value("${auth.jwt.secret}") String encodedSecret) {
        byte[] secret = Base64.getDecoder().decode(encodedSecret);
        if (secret.length < 32) {
            throw new IllegalArgumentException("AUTH_JWT_SECRET must decode to at least 32 bytes");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey authJwtSecretKey) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(authJwtSecretKey));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey authJwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(authJwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> accessTokenType =
                new JwtClaimValidator<>("tokenType", "access"::equals);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("ganzithon"), accessTokenType));
        return decoder;
    }

    @Bean
    JwtDecoder refreshJwtDecoder(SecretKey authJwtSecretKey) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(authJwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256).build();
        OAuth2TokenValidator<org.springframework.security.oauth2.jwt.Jwt> refreshTokenType =
                new JwtClaimValidator<>("tokenType", "refresh"::equals);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer("ganzithon"), refreshTokenType));
        return decoder;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    AuthUseCase authUseCase(
            AuthUserStore users,
            RefreshTokenStore refreshTokens,
            PasswordHasher passwordHasher,
            AuthTokenIssuer tokenIssuer,
            SocialProfileClient socialProfileClient,
            MalangiInventoryStore malangiInventory,
            AuthTransaction transaction
    ) {
        return new AuthService(users, refreshTokens, passwordHasher, tokenIssuer, socialProfileClient,
                malangiInventory, transaction);
    }

    @Bean
    AuthTransaction authTransaction(PlatformTransactionManager transactionManager) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        return new AuthTransaction() {
            @Override
            public <T> T required(Supplier<T> work) {
                return template.execute(status -> work.get());
            }
        };
    }
}
