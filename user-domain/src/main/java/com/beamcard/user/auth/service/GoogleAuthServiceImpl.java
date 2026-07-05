package com.beamcard.user.auth.service;

import com.beamcard.user.auth.exception.InvalidGoogleTokenException;
import com.beamcard.user.auth.exception.OAuthEmailConflictException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import com.beamcard.user.auth.service.GoogleIdentityVerifier.GoogleIdentity;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Slf4j
public class GoogleAuthServiceImpl implements GoogleAuthService {

    private static final String DEFAULT_LOCALE = "en";
    private static final int MAX_USERNAME_ATTEMPTS = 10;

    private final UserRepository userRepository;
    private final UsernameRepository usernameRepository;
    private final GoogleIdentityVerifier googleIdentityVerifier;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public GoogleAuthResult authenticate(GoogleAuthCommand command) {
        GoogleIdentity identity = googleIdentityVerifier.verify(command.idToken());
        if (!identity.emailVerified()) {
            throw new InvalidGoogleTokenException("Google account email is not verified");
        }

        Optional<User> existing = userRepository.findByGoogleSub(identity.sub());
        if (existing.isPresent()) {
            return issueFor(existing.get(), false);
        }
        if (userRepository.existsByEmail(identity.email())) {
            throw new OAuthEmailConflictException(identity.email());
        }
        return issueFor(createUser(identity, command.fallbackLocale()), true);
    }

    private User createUser(GoogleIdentity identity, String fallbackLocale) {
        User saved = userRepository.save(User.builder()
                .email(identity.email())
                .passwordHash(null) // OAuth-only
                .googleSub(identity.sub())
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .locale(normalizeLocale(fallbackLocale))
                .build());
        String username = reservePlaceholderUsername(saved.getId());
        log.debug("Created Google user {} with placeholder handle {}", saved.getId(), username);
        return saved;
    }

    private String reservePlaceholderUsername(UUID userId) {
        for (int attempt = 0; attempt < MAX_USERNAME_ATTEMPTS; attempt++) {
            String candidate = "user_" + UUID.randomUUID().toString().substring(0, 8);
            if (!usernameRepository.existsByUsername(candidate)) {
                usernameRepository.save(candidate, userId);
                return candidate;
            }
        }
        throw new IllegalStateException("Could not generate a unique placeholder username");
    }

    private GoogleAuthResult issueFor(User user, boolean needsUsername) {
        String username = usernameRepository
                .findUsernameByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("user %s has no username".formatted(user.getId())));
        JwtService.IssuedToken token = jwtService.issueAccessToken(user, username);
        String refreshToken = refreshTokenService.issueRefreshToken(user.getId());
        log.debug("Google sign-in succeeded for user {} ({})", user.getId(), user.getEmail());
        return new GoogleAuthResult(user, username, token, refreshToken, needsUsername);
    }

    private static String normalizeLocale(String locale) {
        return locale != null && locale.matches("en|de|uk") ? locale : DEFAULT_LOCALE;
    }
}
