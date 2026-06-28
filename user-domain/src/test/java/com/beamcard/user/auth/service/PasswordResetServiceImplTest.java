package com.beamcard.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.beamcard.user.auth.exception.InvalidResetTokenException;
import com.beamcard.user.auth.model.PasswordResetToken;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.repository.PasswordResetTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceImplTest {

    private static final String URL_TEMPLATE = "https://beamcard.app/reset-password?token=%s";

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordResetTokenRepository tokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    EmailSender emailSender;

    PasswordResetServiceImpl service;

    UUID userId;
    User activeUser;

    @BeforeEach
    void setUp() {
        service = new PasswordResetServiceImpl(
                userRepository, tokenRepository, passwordEncoder, emailSender, Duration.ofMinutes(30), URL_TEMPLATE);
        userId = UUID.randomUUID();
        activeUser = User.builder()
                .id(userId)
                .email("alice@example.com")
                .passwordHash("$2a$12$old")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void requestReset_knownActiveUser_storesHashedTokenAndEmailsRawLink() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.requestReset("alice@example.com");

        verify(tokenRepository).deleteByUserId(userId);

        ArgumentCaptor<PasswordResetToken> saved = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(saved.capture());
        ArgumentCaptor<String> emailedUrl = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendPasswordReset(eq("alice@example.com"), emailedUrl.capture());

        String rawToken = emailedUrl.getValue().substring(emailedUrl.getValue().indexOf("token=") + 6);
        assertThat(saved.getValue().getTokenHash()).isEqualTo(sha256Hex(rawToken));
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now());
        assertThat(saved.getValue().getUsedAt()).isNull();
    }

    @Test
    void requestReset_unknownEmail_isSilentNoOp() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        service.requestReset("ghost@example.com");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailSender);
    }

    @Test
    void requestReset_nonActiveUser_isSilentNoOp() {
        when(userRepository.findByEmail("alice@example.com"))
                .thenReturn(Optional.of(activeUser.withStatus(UserStatus.SUSPENDED)));

        service.requestReset("alice@example.com");

        verify(tokenRepository, never()).save(any());
        verifyNoInteractions(emailSender);
    }

    @Test
    void resetPassword_validToken_setsNewHashAndMarksUsed() {
        String rawToken = "raw-token-value";
        PasswordResetToken token = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(tokenRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.encode("brandnewpassword")).thenReturn("$2a$12$new");

        service.resetPassword(rawToken, "brandnewpassword");

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getPasswordHash()).isEqualTo("$2a$12$new");

        ArgumentCaptor<PasswordResetToken> savedToken = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(savedToken.capture());
        assertThat(savedToken.getValue().getUsedAt()).isNotNull();
    }

    @Test
    void resetPassword_unknownToken_throwsAndLeavesPasswordUnchanged() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resetPassword("nope", "brandnewpassword"))
                .isInstanceOf(InvalidResetTokenException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_expiredToken_throwsAndLeavesPasswordUnchanged() {
        String rawToken = "expired-token";
        PasswordResetToken expired = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(tokenRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.resetPassword(rawToken, "brandnewpassword"))
                .isInstanceOf(InvalidResetTokenException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void resetPassword_alreadyUsedToken_throws() {
        String rawToken = "used-token";
        PasswordResetToken used = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(sha256Hex(rawToken))
                .expiresAt(Instant.now().plusSeconds(600))
                .usedAt(Instant.now().minusSeconds(5))
                .build();
        when(tokenRepository.findByTokenHash(sha256Hex(rawToken))).thenReturn(Optional.of(used));

        assertThatThrownBy(() -> service.resetPassword(rawToken, "brandnewpassword"))
                .isInstanceOf(InvalidResetTokenException.class);

        verify(userRepository, never()).save(any());
    }

    private static String sha256Hex(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
