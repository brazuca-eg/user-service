package com.beamcard.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beamcard.user.auth.exception.IncorrectPasswordException;
import com.beamcard.user.auth.exception.PasswordNotSetException;
import com.beamcard.user.auth.exception.UsernameAlreadyExistsException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.repository.RefreshTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import com.beamcard.user.auth.service.AccountService.AccountUpdateResult;
import com.beamcard.user.auth.service.JwtService.IssuedToken;
import java.util.Optional;
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
class AccountServiceImplTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UsernameRepository usernameRepository;

    @Mock
    JwtService jwtService;

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @InjectMocks
    AccountServiceImpl service;

    UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        lenientCommon();
    }

    private User user(String locale) {
        return User.builder()
                .id(userId)
                .email("alice@example.com")
                .passwordHash("$2a$hash")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .locale(locale)
                .build();
    }

    private void lenientCommon() {
        lenient().when(jwtService.issueAccessToken(any(), any())).thenReturn(new IssuedToken("jwt", 900));
        lenient().when(refreshTokenService.issueRefreshToken(userId)).thenReturn("refresh");
    }

    @Test
    void changesUsername_whenAvailable() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("en")));
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("user_0a1b2c3d"));
        when(usernameRepository.existsByUsername("alice")).thenReturn(false);

        AccountUpdateResult result = service.updateAccount(userId, "alice", null);

        assertThat(result.username()).isEqualTo("alice");
        verify(usernameRepository).changeUsername(userId, "alice");
    }

    @Test
    void rejectsUsername_whenTaken() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("en")));
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("user_0a1b2c3d"));
        when(usernameRepository.existsByUsername("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.updateAccount(userId, "taken", null))
                .isInstanceOf(UsernameAlreadyExistsException.class);
        verify(usernameRepository, never()).changeUsername(any(), any());
    }

    @Test
    void changesLocale_persistsNewValue() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("en")));
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("alice"));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountUpdateResult result = service.updateAccount(userId, null, "de");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getLocale()).isEqualTo("de");
        assertThat(result.user().getLocale()).isEqualTo("de");
        verify(usernameRepository, never()).changeUsername(any(), any());
    }

    @Test
    void noChange_leavesHandleAndLocale_butStillIssuesTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("en")));
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("alice"));

        AccountUpdateResult result = service.updateAccount(userId, "alice", "en");

        assertThat(result.refreshToken()).isEqualTo("refresh");
        verify(usernameRepository, never()).changeUsername(any(), any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void sameHandleDifferentCase_isTreatedAsNoChange() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("en")));
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("Alice"));

        service.updateAccount(userId, "alice", null);

        verify(usernameRepository, never()).existsByUsername(eq("alice"));
        verify(usernameRepository, never()).changeUsername(any(), any());
    }

    @Test
    void changePassword_updatesHash_revokesSessions_andReissuesTokens() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("en")));
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("alice"));
        when(passwordEncoder.matches("current-pass", "$2a$hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password-123")).thenReturn("$2a$newhash");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AccountUpdateResult result = service.changePassword(userId, "current-pass", "new-password-123");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getPasswordHash()).isEqualTo("$2a$newhash");
        verify(refreshTokenRepository).revokeAllForUser(userId);
        assertThat(result.refreshToken()).isEqualTo("refresh");
    }

    @Test
    void changePassword_rejects_whenCurrentPasswordWrong() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("en")));
        when(passwordEncoder.matches("wrong", "$2a$hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(userId, "wrong", "new-password-123"))
                .isInstanceOf(IncorrectPasswordException.class);
        verify(userRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeAllForUser(any());
    }

    @Test
    void changePassword_rejects_whenAccountHasNoPassword() {
        User googleOnly = User.builder()
                .id(userId)
                .email("g@example.com")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .locale("en")
                .build(); // no passwordHash
        when(userRepository.findById(userId)).thenReturn(Optional.of(googleOnly));

        assertThatThrownBy(() -> service.changePassword(userId, "x", "new-password-123"))
                .isInstanceOf(PasswordNotSetException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteAccount_softDeletes_revokesTokens_andReleasesUsername() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user("en")));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deleteAccount(userId);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(UserStatus.DELETED);
        verify(refreshTokenRepository).revokeAllForUser(userId);
        verify(usernameRepository).deleteByUserId(userId);
    }
}
