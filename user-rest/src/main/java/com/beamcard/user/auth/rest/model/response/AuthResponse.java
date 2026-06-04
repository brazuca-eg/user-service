package com.beamcard.user.auth.rest.model.response;

import java.util.UUID;

public record AuthResponse(String accessToken, String tokenType, long expiresIn, UserSummary user) {

    public record UserSummary(UUID id, String email, String username, String plan) {}
}
