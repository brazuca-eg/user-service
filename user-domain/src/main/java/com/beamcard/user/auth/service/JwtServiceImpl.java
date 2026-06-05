package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.SigningKey;
import com.beamcard.user.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

    private final SigningKey signingKey;
    private final Duration accessTokenTtl;

    public JwtServiceImpl(SigningKey signingKey, @Value("${beamcard.auth.access-token-ttl}") Duration accessTokenTtl) {
        this.signingKey = signingKey;
        this.accessTokenTtl = accessTokenTtl;
    }

    @Override
    public IssuedToken issueAccessToken(User user, String username) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenTtl);

        String jwt = Jwts.builder()
                .header()
                .keyId(signingKey.keyId())
                .and()
                .subject(user.getId().toString())
                .claim("username", username)
                .claim("plan", user.getPlan().name().toLowerCase())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(signingKey.keyPair().getPrivate(), Jwts.SIG.RS256)
                .compact();

        return new IssuedToken(jwt, accessTokenTtl.toSeconds());
    }

    @Override
    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey.keyPair().getPublic())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return new AuthenticatedUser(
                UUID.fromString(claims.getSubject()),
                claims.get("username", String.class),
                claims.get("plan", String.class));
    }
}
