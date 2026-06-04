package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;
import java.util.UUID;

public interface JwtService {

    IssuedToken issueAccessToken(User user, String username);

    /**
     * Verifies signature + expiry of an access token and extracts its claims.
     */
    AuthenticatedUser verify(String token);

    record IssuedToken(String value, long expiresInSeconds) {}

    record AuthenticatedUser(UUID id, String username, String plan) {}
}
