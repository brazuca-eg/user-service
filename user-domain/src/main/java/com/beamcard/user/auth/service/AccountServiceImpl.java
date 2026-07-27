package com.beamcard.user.auth.service;

import com.beamcard.user.auth.exception.IncorrectPasswordException;
import com.beamcard.user.auth.exception.PasswordNotSetException;
import com.beamcard.user.auth.exception.UserNotFoundException;
import com.beamcard.user.auth.exception.UsernameAlreadyExistsException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.repository.RefreshTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
@Slf4j
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final UsernameRepository usernameRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AccountView getById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        String username = usernameRepository
                .findUsernameByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("user %s has no username".formatted(userId)));
        return new AccountView(user, username);
    }

    @Override
    public AccountView getByUsername(String username) {
        UUID userId = usernameRepository
                .findUserIdByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return new AccountView(user, username);
    }

    @Override
    @Transactional
    public AccountUpdateResult updateAccount(UUID userId, String newUsername, String newLocale) {
        var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        var username = usernameRepository
                .findUsernameByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("user %s has no username".formatted(userId)));

        if (StringUtils.hasText(newUsername) && !newUsername.equalsIgnoreCase(username)) {
            if (usernameRepository.existsByUsername(newUsername)) {
                throw new UsernameAlreadyExistsException(newUsername);
            }
            usernameRepository.changeUsername(userId, newUsername);
            username = newUsername;
        }

        if (StringUtils.hasText(newLocale) && !newLocale.equals(user.getLocale())) {
            user = userRepository.save(user.withLocale(newLocale));
        }

        JwtService.IssuedToken token = jwtService.issueAccessToken(user, username);
        var refreshToken = refreshTokenService.issueRefreshToken(userId);
        log.debug("Account updated for user {} (handle {})", userId, username);
        return new AccountUpdateResult(user, username, token, refreshToken);
    }

    @Override
    @Transactional
    public AccountUpdateResult changePassword(UUID userId, String currentPassword, String newPassword) {
        var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (!StringUtils.hasText(user.getPasswordHash())) {
            throw new PasswordNotSetException(); // Google-only account; nothing to verify against
        }
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IncorrectPasswordException();
        }

        var updated = userRepository.save(user.withPasswordHash(passwordEncoder.encode(newPassword)));
        refreshTokenRepository.revokeAllForUser(userId);
        var username = usernameRepository
                .findUsernameByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("user %s has no username".formatted(userId)));
        JwtService.IssuedToken token = jwtService.issueAccessToken(updated, username);
        var refreshToken = refreshTokenService.issueRefreshToken(userId);
        log.info("Password changed for user {}", userId);
        return new AccountUpdateResult(updated, username, token, refreshToken);
    }

    @Override
    @Transactional
    public void deleteAccount(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        userRepository.save(user.withStatus(UserStatus.DELETED));
        refreshTokenRepository.revokeAllForUser(userId);
        usernameRepository.deleteByUserId(userId);
        log.info("Account soft-deleted for user {}", userId);
    }
}
