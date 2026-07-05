package com.beamcard.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beamcard.user.auth.exception.InvalidGoogleTokenException;
import com.beamcard.user.auth.exception.OAuthEmailConflictException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import com.beamcard.user.auth.service.GoogleAuthService.GoogleAuthCommand;
import com.beamcard.user.auth.service.GoogleAuthService.GoogleAuthResult;
import com.beamcard.user.auth.service.GoogleIdentityVerifier.GoogleIdentity;
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

@ExtendWith(MockitoExtension.class)
class GoogleAuthServiceImplTest {

    private static final String SUB = "google-sub-123";
    private static final String EMAIL = "alice@example.com";

    @Mock
    UserRepository userRepository;

    @Mock
    UsernameRepository usernameRepository;

    @Mock
    GoogleIdentityVerifier googleIdentityVerifier;

    @Mock
    JwtService jwtService;

    @Mock
    RefreshTokenService refreshTokenService;

    @InjectMocks
    GoogleAuthServiceImpl service;

    UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private User existingUser() {
        return User.builder()
                .id(userId)
                .email(EMAIL)
                .googleSub(SUB)
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .locale("en")
                .build();
    }

    private void verified() {
        when(googleIdentityVerifier.verify("tok")).thenReturn(new GoogleIdentity(SUB, EMAIL, true));
    }

    @Test
    void returningUser_logsIn_withoutNeedingUsername() {
        verified();
        when(userRepository.findByGoogleSub(SUB)).thenReturn(Optional.of(existingUser()));
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("alice"));
        when(jwtService.issueAccessToken(any(), eq("alice"))).thenReturn(new IssuedToken("jwt", 900));
        when(refreshTokenService.issueRefreshToken(userId)).thenReturn("refresh");

        GoogleAuthResult result = service.authenticate(new GoogleAuthCommand("tok", "de"));

        assertThat(result.needsUsername()).isFalse();
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.token().value()).isEqualTo("jwt");
        verify(userRepository, never()).save(any());
    }

    @Test
    void newUser_isCreatedWithPlaceholderHandle_andNeedsUsername() {
        verified();
        when(userRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any())).thenReturn(existingUser());
        when(usernameRepository.existsByUsername(anyString())).thenReturn(false);
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("user_abc12345"));
        when(jwtService.issueAccessToken(any(), anyString())).thenReturn(new IssuedToken("jwt", 900));
        when(refreshTokenService.issueRefreshToken(userId)).thenReturn("refresh");

        GoogleAuthResult result = service.authenticate(new GoogleAuthCommand("tok", "de"));

        assertThat(result.needsUsername()).isTrue();

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getGoogleSub()).isEqualTo(SUB);
        assertThat(savedUser.getValue().getPasswordHash()).isNull(); // OAuth-only account
        assertThat(savedUser.getValue().getLocale()).isEqualTo("de"); // fallback locale applied

        ArgumentCaptor<String> reserved = ArgumentCaptor.forClass(String.class);
        verify(usernameRepository).save(reserved.capture(), eq(userId));
        assertThat(reserved.getValue()).startsWith("user_");
    }

    @Test
    void nullFallbackLocale_defaultsToEnglish() {
        verified();
        when(userRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(userRepository.save(any())).thenReturn(existingUser());
        when(usernameRepository.existsByUsername(anyString())).thenReturn(false);
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("user_abc12345"));
        when(jwtService.issueAccessToken(any(), anyString())).thenReturn(new IssuedToken("jwt", 900));
        when(refreshTokenService.issueRefreshToken(userId)).thenReturn("refresh");

        service.authenticate(new GoogleAuthCommand("tok", null));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertThat(savedUser.getValue().getLocale()).isEqualTo("en");
    }

    @Test
    void emailAlreadyRegisteredWithPassword_isRejected() {
        verified();
        when(userRepository.findByGoogleSub(SUB)).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.authenticate(new GoogleAuthCommand("tok", "en")))
                .isInstanceOf(OAuthEmailConflictException.class);
        verify(userRepository, never()).save(any());
    }

    @Test
    void unverifiedGoogleEmail_isRejected() {
        when(googleIdentityVerifier.verify("tok")).thenReturn(new GoogleIdentity(SUB, EMAIL, false));

        assertThatThrownBy(() -> service.authenticate(new GoogleAuthCommand("tok", "en")))
                .isInstanceOf(InvalidGoogleTokenException.class);
        verify(userRepository, never()).save(any());
    }
}
