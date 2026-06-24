package com.beamcard.user.auth.exception;

import java.util.UUID;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(UUID id) {
        super("user not found: " + id);
    }

    public UserNotFoundException(String username) {
        super("user not found for username: " + username);
    }
}
