package com.beamcard.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beamcard.user.auth.exception.AccountNotActiveException;
import com.beamcard.user.auth.exception.InvalidCredentialsException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UsernameRepository usernameRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    LoginServiceImpl loginService;

    private UUID userId;
    private User activeUser;
    private LoginService.LoginCommand command;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        activeUser = User.builder()
                .id(userId)
                .email("alice@example.com")
                .passwordHash("$2a$12$hashed")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .build();
        command = new LoginService.LoginCommand("alice@example.com", "correcthorsebatterystaple");
    }

    @Test
    void happyPath_verifiesPasswordAndIssuesToken() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correcthorsebatterystaple", "$2a$12$hashed"))
                .thenReturn(true);
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("alice"));
        when(jwtService.issueAccessToken(activeUser, "alice")).thenReturn(new JwtService.IssuedToken("jwt.value", 900));

        LoginService.LoginResult result = loginService.login(command);

        assertThat(result.user()).isSameAs(activeUser);
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.token().value()).isEqualTo("jwt.value");
    }

    @Test
    void unknownEmail_throwsInvalidCredentials_withoutLeakingExistence() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.login(command)).isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issueAccessToken(any(), any());
    }

    @Test
    void wrongPassword_throwsInvalidCredentials() {
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("correcthorsebatterystaple", "$2a$12$hashed"))
                .thenReturn(false);

        assertThatThrownBy(() -> loginService.login(command)).isInstanceOf(InvalidCredentialsException.class);

        verify(jwtService, never()).issueAccessToken(any(), any());
    }

    @Test
    void inactiveAccount_throwsAccountNotActive() {
        User suspended = activeUser.withStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(suspended));
        when(passwordEncoder.matches("correcthorsebatterystaple", "$2a$12$hashed"))
                .thenReturn(true);

        assertThatThrownBy(() -> loginService.login(command)).isInstanceOf(AccountNotActiveException.class);

        verify(jwtService, never()).issueAccessToken(any(), any());
    }
}
