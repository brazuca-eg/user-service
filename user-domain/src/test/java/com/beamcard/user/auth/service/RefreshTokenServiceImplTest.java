package com.beamcard.user.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.beamcard.user.auth.exception.AccountNotActiveException;
import com.beamcard.user.auth.exception.InvalidRefreshTokenException;
import com.beamcard.user.auth.model.RefreshToken;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.repository.RefreshTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
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
class RefreshTokenServiceImplTest {

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    UsernameRepository usernameRepository;

    @Mock
    JwtService jwtService;

    RefreshTokenServiceImpl service;

    UUID userId;
    User activeUser;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenServiceImpl(
                refreshTokenRepository, userRepository, usernameRepository, jwtService, Duration.ofDays(30));
        userId = UUID.randomUUID();
        activeUser = User.builder()
                .id(userId)
                .email("alice@example.com")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .build();
    }

    @Test
    void issue_storesHashedTokenWithFutureExpiry_andReturnsRaw() {
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        String raw = service.issueRefreshToken(userId);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getTokenHash()).isEqualTo(sha256Hex(raw));
        assertThat(saved.getValue().getUserId()).isEqualTo(userId);
        assertThat(saved.getValue().getExpiresAt()).isAfter(Instant.now());
        assertThat(saved.getValue().getRevokedAt()).isNull();
    }

    @Test
    void refresh_rotates_revokingOld_andIssuingNewPair() {
        String oldRaw = "old-raw-token";
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(sha256Hex(oldRaw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(refreshTokenRepository.findByTokenHash(sha256Hex(oldRaw))).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser));
        when(usernameRepository.findUsernameByUserId(userId)).thenReturn(Optional.of("alice"));
        when(jwtService.issueAccessToken(activeUser, "alice")).thenReturn(new JwtService.IssuedToken("new.jwt", 900));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RefreshTokenService.RefreshResult result = service.refresh(oldRaw);

        assertThat(result.accessToken().value()).isEqualTo("new.jwt");
        assertThat(result.username()).isEqualTo("alice");
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo(oldRaw);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(2)).save(saved.capture());
        assertThat(saved.getAllValues().get(0).getRevokedAt()).isNotNull();
        assertThat(saved.getAllValues().get(0).getTokenHash()).isEqualTo(sha256Hex(oldRaw));
        assertThat(saved.getAllValues().get(1).getRevokedAt()).isNull();
        assertThat(saved.getAllValues().get(1).getTokenHash()).isEqualTo(sha256Hex(result.refreshToken()));
    }

    @Test
    void refresh_unknownToken_throws() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("nope")).isInstanceOf(InvalidRefreshTokenException.class);
        verify(jwtService, never()).issueAccessToken(any(), any());
    }

    @Test
    void refresh_expiredOrRevokedToken_throws() {
        String raw = "stale";
        RefreshToken revoked = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(sha256Hex(raw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .revokedAt(Instant.now().minusSeconds(5))
                .build();
        when(refreshTokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOf(InvalidRefreshTokenException.class);
        verify(jwtService, never()).issueAccessToken(any(), any());
    }

    @Test
    void refresh_inactiveUser_throwsAccountNotActive() {
        String raw = "valid";
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(sha256Hex(raw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(refreshTokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(activeUser.withStatus(UserStatus.SUSPENDED)));

        assertThatThrownBy(() -> service.refresh(raw)).isInstanceOf(AccountNotActiveException.class);
    }

    @Test
    void revoke_marksTokenRevoked() {
        String raw = "to-revoke";
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash(sha256Hex(raw))
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(refreshTokenRepository.findByTokenHash(sha256Hex(raw))).thenReturn(Optional.of(token));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.revoke(raw);

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        assertThat(saved.getValue().getRevokedAt()).isNotNull();
    }

    @Test
    void revoke_unknownToken_isNoOp() {
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty());

        service.revoke("ghost");

        verify(refreshTokenRepository, never()).save(any());
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
