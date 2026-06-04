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
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
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

    @InjectMocks
    SignupServiceImpl signupService;

    SignupService.SignupCommand validCommand;

    @BeforeEach
    void setUp() {
        validCommand = new SignupService.SignupCommand("alice@example.com", "correcthorsebatterystaple", "alice");
    }

    @Test
    void happyPath_persistsUserAndUsername_andReturnsToken() {
        UUID newUserId = UUID.randomUUID();

        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(usernameRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("correcthorsebatterystaple")).thenReturn("$2a$12$hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return u.withId(newUserId);
        });
        when(jwtService.issueAccessToken(any(User.class), any()))
                .thenReturn(new JwtService.IssuedToken("dummy.jwt.token", 900));

        SignupService.SignupResult result = signupService.signup(validCommand);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("alice@example.com");
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("$2a$12$hashed");
        assertThat(userCaptor.getValue().getPlan()).isEqualTo(UserSubscriptionPlan.FREE);
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.ACTIVE);

        verify(usernameRepository).save(eq("alice"), eq(newUserId));

        assertThat(result.token().value()).isEqualTo("dummy.jwt.token");
        assertThat(result.token().expiresInSeconds()).isEqualTo(900);
        assertThat(result.user().getId()).isEqualTo(newUserId);
        assertThat(result.user().getEmail()).isEqualTo("alice@example.com");
        assertThat(result.username()).isEqualTo("alice");
    }

    @Test
    void rejectsDuplicateEmail_andDoesNotTouchUsernameTable() {
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> signupService.signup(validCommand))
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

        assertThatThrownBy(() -> signupService.signup(validCommand))
                .isInstanceOf(UsernameAlreadyExistsException.class)
                .hasMessageContaining("alice");

        verify(userRepository, never()).save(any());
        verify(usernameRepository, never()).save(any(), any());
        verify(jwtService, never()).issueAccessToken(any(), any());
    }
}
