package com.beamcard.user.auth.rest.model.response;

import com.beamcard.user.auth.model.User;
import java.time.Instant;
import java.util.UUID;

public record AccountResponse(UUID id, String email, String username, String plan, Instant createdAt) {

    public static AccountResponse of(User user, String username) {
        return new AccountResponse(
                user.getId(), user.getEmail(), username, user.getPlan().name().toLowerCase(), user.getCreatedAt());
    }
}
