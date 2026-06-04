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
public class User {
    UUID id;
    String email;
    String passwordHash;
    String googleSub;
    UserSubscriptionPlan plan;
    UserStatus status;
    Instant createdAt;
    Instant updatedAt;
}
