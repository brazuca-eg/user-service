package com.beamcard.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beamcard.user.auth.exception.EmailAlreadyExistsException;
import com.beamcard.user.auth.exception.UsernameAlreadyExistsException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SignupServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UsernameRepository usernameRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    EmailVerificationService emailVerificationService;

    SignupService.SignupCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new SignupService.SignupCommand("alice@example.com", "correcthorsebatterystaple", "alice", "en");
    }

    private SignupServiceImpl service(boolean requireEmailVerification) {
        return new SignupServiceImpl(
                userRepository,
                usernameRepository,
                passwordEncoder,
                jwtService,
                refreshTokenService,
                emailVerificationService,
                requireEmailVerification);
    }

    @Test
    void happyPath_verificationRequired_persistsUser_sendsEmail_andIssuesNoSession() {
        UUID newUserId = UUID.randomUUID();
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(usernameRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("correcthorsebatterystaple")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> ((User) inv.getArgument(0)).withId(newUserId));

        SignupService.SignupResult result = service(true).signup(validCommand);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$12$hashed");
        verify(usernameRepository).save(eq("alice"), eq(newUserId));
        verify(emailVerificationService).sendVerification(any(User.class));

        // No session until verified.
        assertThat(result.verificationRequired()).isTrue();
        assertThat(result.token()).isNull();
        assertThat(result.refreshToken()).isNull();
        verify(jwtService, never()).issueAccessToken(any(), any());
        verify(refreshTokenService, never()).issueRefreshToken(any());
    }

    @Test
    void whenVerificationDisabled_autoLogsInWithTokens() {
        UUID newUserId = UUID.randomUUID();
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(usernameRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("correcthorsebatterystaple")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> ((User) inv.getArgument(0)).withId(newUserId));
        when(jwtService.issueAccessToken(any(User.class), any()))
                .thenReturn(new JwtService.IssuedToken("dummy.jwt.token", 900));
        when(refreshTokenService.issueRefreshToken(newUserId)).thenReturn("refresh.value");

        SignupService.SignupResult result = service(false).signup(validCommand);

        assertThat(result.verificationRequired()).isFalse();
        assertThat(result.token().value()).isEqualTo("dummy.jwt.token");
        assertThat(result.refreshToken()).isEqualTo("refresh.value");
        verify(emailVerificationService).sendVerification(any(User.class));
    }

    @Test
    void rejectsDuplicateEmail_andDoesNotTouchUsernameTable() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service(true).signup(validCommand))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("alice@example.com");

        verify(userRepository, never()).save(any());
        verify(usernameRepository, never()).save(any(), any());
        verify(jwtService, never()).issueAccessToken(any(), any());
    }

    @Test
    void rejectsDuplicateUsername_andDoesNotPersistUser() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(usernameRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> service(true).signup(validCommand))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("alice");

        verify(userRepository, never()).save(any());
        verify(usernameRepository, never()).save(any(), any());
        verify(jwtService, never()).issueAccessToken(any(), any());
    }
}
