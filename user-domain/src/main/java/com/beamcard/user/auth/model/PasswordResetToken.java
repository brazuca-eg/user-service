package com.beamcard.user.auth.model;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.With;

@Getter
@Builder
@AllArgsConstructor
@With
public class PasswordResetToken {
    UUID id;
    UUID userId;
    String tokenHash;
    Instant expiresAt;
    Instant usedAt;
    Instant createdAt;

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }
}
