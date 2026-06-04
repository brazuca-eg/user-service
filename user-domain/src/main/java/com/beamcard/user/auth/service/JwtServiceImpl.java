package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;
import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtServiceImpl implements JwtService {

    private final KeyPair keyPair;
    private final Duration accessTokenTtl;

    public JwtServiceImpl(KeyPair jwtKeyPair, @Value("${beamcard.auth.access-token-ttl}") Duration accessTokenTtl) {
        this.keyPair = jwtKeyPair;
        this.accessTokenTtl = accessTokenTtl;
    }

    @Override
    public IssuedToken issueAccessToken(User user, String username) {
        Instant now = Instant.now();
        Instant exp = now.plus(accessTokenTtl);

        String jwt = Jwts.builder()
                .subject(user.getId().toString())
                .claim("username", username)
                .claim("plan", user.getPlan().name().toLowerCase())
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(keyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        return new IssuedToken(jwt, accessTokenTtl.toSeconds());
    }
}
