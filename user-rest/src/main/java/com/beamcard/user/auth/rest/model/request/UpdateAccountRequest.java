package com.beamcard.user.auth.rest.model.request;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAccountRequest(
        @Size(min = 3, max = 20) @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "invalid_username") String username,
        @Pattern(regexp = "en|de|uk") String locale) {}
