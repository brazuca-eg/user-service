package com.beamcard.user.auth.rest.model.response;

import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.service.JwtService;
import java.util.UUID;

public record AuthResponse(
        String accessToken, String tokenType, long expiresIn, String refreshToken, UserSummary user) {

    public record UserSummary(UUID id, String email, String username, String plan) {}

    public static AuthResponse of(User user, String username, JwtService.IssuedToken token, String refreshToken) {
        return new AuthResponse(
                token.value(),
                "Bearer",
                token.expiresInSeconds(),
                refreshToken,
                new UserSummary(
                        user.getId(),
                        user.getEmail(),
                        username,
                        user.getPlan().name().toLowerCase()));
    }
}
