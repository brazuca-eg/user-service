package com.beamcard.user.auth.service;

import com.beamcard.user.auth.exception.InvalidVerificationTokenException;
import com.beamcard.user.auth.exception.UserNotFoundException;
import com.beamcard.user.auth.model.EmailVerificationToken;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.repository.EmailVerificationTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int TOKEN_BYTES = 32;

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailSender emailSender;
    private final Duration tokenTtl;
    private final String verifyUrlTemplate;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder tokenEncoder = Base64.getUrlEncoder().withoutPadding();

    @Override
    @Transactional
    public void sendVerification(User user) {
        if (user.isEmailVerified()) {
            return;
        }
        tokenRepository.deleteByUserId(user.getId());

        String rawToken = generateRawToken();
        tokenRepository.save(EmailVerificationToken.builder()
                .userId(user.getId())
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(Instant.now().plus(tokenTtl))
                .build());

        emailSender.sendEmailVerification(user.getEmail(), verifyUrlTemplate.formatted(rawToken));
        log.info("Verification email dispatched for user {}.", user.getId());
    }

    @Override
    @Transactional
    public void resend(java.util.UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        sendVerification(user);
    }

    @Override
    @Transactional
    public void resendByEmail(String email) {
        // Anti-enumeration: always succeed; only actually send for an active, unverified account.
        userRepository
                .findByEmail(email)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE && !user.isEmailVerified())
                .ifPresent(this::sendVerification);
    }

    @Override
    @Transactional
    public void confirm(String rawToken) {
        EmailVerificationToken token = tokenRepository
                .findByTokenHash(sha256Hex(rawToken))
                .orElseThrow(InvalidVerificationTokenException::new);

        if (!token.isUsable(Instant.now())) {
            throw new InvalidVerificationTokenException();
        }

        User user = userRepository.findById(token.getUserId()).orElseThrow(InvalidVerificationTokenException::new);
        if (!user.isEmailVerified()) {
            userRepository.save(user.withEmailVerified(true));
        }
        tokenRepository.save(token.withUsedAt(Instant.now()));
        log.info("Email verified for user {}.", user.getId());
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
