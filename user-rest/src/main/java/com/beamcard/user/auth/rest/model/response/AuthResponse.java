package com.beamcard.user.auth.rest.model.response;

import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.service.JwtService;
import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken,
        boolean needsUsername,
        UserSummary user) {

    public record UserSummary(UUID id, String email, String username, String plan, String locale) {}

    public static AuthResponse of(User user, String username, JwtService.IssuedToken token, String refreshToken) {
        return of(user, username, token, refreshToken, false);
    }

    public static AuthResponse of(
            User user, String username, JwtService.IssuedToken token, String refreshToken, boolean needsUsername) {
        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                refreshToken,
                needsUsername,
                new UserSummary(
                        user.getId(),
                        user.getEmail(),
                        username,
                        user.getPlan().name().toLowerCase(),
                        user.getLocale()));
    }
}
