package com.beamcard.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceImplTest {

    private static final Duration TTL = Duration.ofMinutes(15);

    private KeyPair keyPair;
    private JwtServiceImpl jwtService;
    private User user;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keyPair = generator.generateKeyPair();

        jwtService = new JwtServiceImpl(keyPair, TTL);

        user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .plan(UserSubscriptionPlan.PREMIUM)
                .build();
    }

    @Test
    void issuesTokenWithExpectedClaims() {
        JwtService.IssuedToken token = jwtService.issueAccessToken(user, "alice");

        Claims claims = parse(token.value(), keyPair);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("username", String.class)).isEqualTo("alice");
        assertThat(claims.get("plan", String.class)).isEqualTo("premium");
    }

    @Test
    void reportsTtlInSeconds() {
        JwtService.IssuedToken token = jwtService.issueAccessToken(user, "alice");

        assertThat(token.expiresInSeconds()).isEqualTo(TTL.toSeconds());
    }

    @Test
    void setsExpirationOneTtlAfterIssuedAt() {
        JwtService.IssuedToken token = jwtService.issueAccessToken(user, "alice");

        Claims claims = parse(token.value(), keyPair);
        long elapsedMillis =
                claims.getExpiration().getTime() - claims.getIssuedAt().getTime();

        assertThat(elapsedMillis).isEqualTo(TTL.toMillis());
    }

    @Test
    void tokenSignedWithOurKeyFailsVerificationUnderADifferentKey() throws NoSuchAlgorithmException {
        JwtService.IssuedToken token = jwtService.issueAccessToken(user, "alice");

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair attackerKeyPair = generator.generateKeyPair();

        assertThatThrownBy(() -> parse(token.value(), attackerKeyPair)).isInstanceOf(SignatureException.class);
    }

    private static Claims parse(String jwt, KeyPair keyPair) {
        return Jwts.parser()
                .verifyWith(keyPair.getPublic())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}
