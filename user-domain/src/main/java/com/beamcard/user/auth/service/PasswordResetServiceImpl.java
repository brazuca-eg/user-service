package com.beamcard.user.auth.service;

import com.beamcard.user.auth.exception.InvalidResetTokenException;
import com.beamcard.user.auth.model.PasswordResetToken;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.repository.PasswordResetTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailSender emailSender;
    private final Duration tokenTtl;
    private final String resetUrlTemplate;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder tokenEncoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    @Transactional
    public void requestReset(String email) {
        Optional<User> found = userRepository.findByEmail(email);
        if (found.isEmpty()) {
            log.info("Password reset requested for an unknown email");
            return;
        }
        User user = found.get();
        if (user.getStatus() != UserStatus.ACTIVE) {
            log.info("Password reset requested for non-active user {}", user.getId());
            return;
        }

        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateRawToken();
        PasswordResetToken token = PasswordResetToken.builder()
                .userId(user.getId())
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(Instant.now().plus(tokenTtl))
                .build();
        tokenRepository.save(token);

        emailSender.sendPasswordReset(user.getEmail(), resetUrlTemplate.formatted(rawToken));
        log.info("Password reset email dispatched for user {}.", user.getId());
    }

    @Override
    @Transactional
    public void resetPassword(String rawToken, String newRawPassword) {
        PasswordResetToken token =
                tokenRepository.findByTokenHash(sha256Hex(rawToken)).orElseThrow(InvalidResetTokenException::new);

        if (!token.isUsable(Instant.now())) {
            throw new InvalidResetTokenException();
        }

        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidResetTokenException::new);

        userRepository.save(user.withPasswordHash(passwordEncoder.encode(newRawPassword)));
        tokenRepository.save(token.withUsedAt(Instant.now()));
        log.info("Password reset completed for user {}.", user.getId());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return tokenEncoder.encodeToString(bytes);
    }

    /** Hash the high-entropy random token with SHA-256 */
    private static String sha256Hex(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
