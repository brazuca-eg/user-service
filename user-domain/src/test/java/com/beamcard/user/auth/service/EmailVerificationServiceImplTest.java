package com.beamcard.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beamcard.user.auth.exception.InvalidVerificationTokenException;
import com.beamcard.user.auth.model.EmailVerificationToken;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.repository.EmailVerificationTokenRepository;
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

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    private static final String URL_TEMPLATE = "https://beamcard.app/verify-email?token=%s";

    @Mock
    UserRepository userRepository;

    @Mock
    EmailVerificationTokenRepository tokenRepository;

    @Mock
    EmailSender emailSender;

    EmailVerificationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EmailVerificationServiceImpl(
                userRepository, tokenRepository, emailSender, Duration.ofHours(24), URL_TEMPLATE);
    }

    private static User user(UUID id, boolean verified) {
        return User.builder()
                .id(id)
                .email("alice@example.com")
                .emailVerified(verified)
                .build();
    }

    @Test
    void sendVerification_clearsPriorTokens_savesHashedToken_andEmailsLink() {
        User user = user(UUID.randomUUID(), false);
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.sendVerification(user);

        verify(tokenRepository).deleteByUserId(user.getId());
        ArgumentCaptor<String> url = ArgumentCaptor.forClass(String.class);
        verify(emailSender).sendEmailVerification(eq("alice@example.com"), url.capture());
        String rawToken = url.getValue().substring(url.getValue().indexOf("token=") + "token=".length());

        ArgumentCaptor<EmailVerificationToken> saved = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(sha256Hex(rawToken));
    }

    @Test
    void sendVerification_isNoOp_whenAlreadyVerified() {
        service.sendVerification(user(UUID.randomUUID(), true));

        verify(tokenRepository, never()).save(any());
        verify(emailSender, never()).sendEmailVerification(any(), any());
    }

    @Test
    void resendByEmail_sends_forActiveUnverifiedUser() {
        User unverified = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .status(com.beamcard.user.auth.model.UserStatus.ACTIVE)
                .emailVerified(false)
                .build();
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(unverified));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.resendByEmail("alice@example.com");

        verify(emailSender).sendEmailVerification(eq("alice@example.com"), any());
    }

    @Test
    void resendByEmail_isSilentNoOp_forUnknownEmail() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        service.resendByEmail("ghost@example.com"); // must not throw (anti-enumeration)

        verify(emailSender, never()).sendEmailVerification(any(), any());
    }

    @Test
    void confirm_marksUserVerified_andTokenUsed() {
        UUID userId = UUID.randomUUID();
        String raw = "raw-verification-token";
        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(userId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(tokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, false)));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.confirm(raw);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().isEmailVerified()).isTrue();
        ArgumentCaptor<EmailVerificationToken> savedToken = ArgumentCaptor.forClass(EmailVerificationToken.class);
        verify(tokenRepository).save(savedToken.capture());
        assertThat(savedToken.getValue().getUsedAt()).isNotNull();
    }

    @Test
    void confirm_throws_whenTokenUnknown() {
        when(tokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm("nope")).isInstanceOf(InvalidVerificationTokenException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void confirm_throws_whenTokenExpired() {
        String raw = "raw";
        EmailVerificationToken expired = EmailVerificationToken.builder()
                .userId(UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(1))
                .build();
        when(tokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.confirm(raw)).isInstanceOf(InvalidVerificationTokenException.class);
    }

    private static String sha256Hex(String value) {
        try {
            return HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
