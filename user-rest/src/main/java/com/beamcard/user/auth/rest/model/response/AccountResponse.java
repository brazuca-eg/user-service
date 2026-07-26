package com.beamcard.user.auth.rest.model.response;

import com.beamcard.user.auth.model.User;
import java.time.Instant;
import java.util.UUID;
import org.springframework.util.StringUtils;

public record AccountResponse(
        UUID id,
        String email,
        String username,
        String plan,
        String locale,
        Instant createdAt,
        boolean hasPassword,
        boolean emailVerified) {

    public static AccountResponse of(User user, String username) {
        return new AccountResponse(
                user.getId(),
                user.getEmail(),
                username,
                user.getPlan().name().toLowerCase(),
                user.getLocale(),
                user.getCreatedAt(),
                StringUtils.hasText(user.getPasswordHash()), // false for Google-only accounts
                user.isEmailVerified());
    }
}
