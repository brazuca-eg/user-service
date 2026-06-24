package com.beamcard.user.client;

import java.util.UUID;

/**
 * Minimal user view exposed to other services via {@link UserClient}.
 */
public record UserSummary(UUID id, String username, String plan, String status) {}
