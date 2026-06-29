package com.beamcard.user.auth.service;

import com.beamcard.user.auth.exception.AccountNotActiveException;
import com.beamcard.user.auth.exception.InvalidRefreshTokenException;
import com.beamcard.user.auth.model.RefreshToken;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.repository.RefreshTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UsernameRepository usernameRepository;
    private final JwtService jwtService;
    private final Duration tokenTtl;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder tokenEncoder = Base64.getUrlEncoder().withoutPadding();

    public RefreshTokenServiceImpl(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            UsernameRepository usernameRepository,
            JwtService jwtService,
            Duration tokenTtl) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.usernameRepository = usernameRepository;
        this.jwtService = jwtService;
        this.tokenTtl = tokenTtl;
    }

    @Override
    @Transactional
    public String issueRefreshToken(UUID userId) {
        String rawToken = generateRawToken();
        refreshTokenRepository.save(RefreshToken.builder()
                .userId(userId)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(Instant.now().plus(tokenTtl))
                .build());
        return rawToken;
    }

    @Override
    @Transactional
    public RefreshResult refresh(String rawRefreshToken) {
        RefreshToken token = refreshTokenRepository
                .findByTokenHash(sha256Hex(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (!token.isUsable(Instant.now())) {
            throw new InvalidRefreshTokenException();
        }

        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidRefreshTokenException::new);
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }
        String username = usernameRepository
                .findUsernameByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("user %s has no username".formatted(user.getId())));

        refreshTokenRepository.save(token.withRevokedAt(Instant.now()));
        String newRefreshToken = issueRefreshToken(user.getId());
        JwtService.IssuedToken accessToken = jwtService.issueAccessToken(user, username);
        log.info("Refreshed tokens for user {}.", user.getId());

        return new RefreshResult(user, username, accessToken, newRefreshToken);
    }

    @Override
    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(sha256Hex(rawRefreshToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                refreshTokenRepository.save(token.withRevokedAt(Instant.now()));
                log.info("Revoked refresh token for user {}.", token.getUserId());
            }
        });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return tokenEncoder.encodeToString(bytes);
    }

    private static String sha256Hex(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
