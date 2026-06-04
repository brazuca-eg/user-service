package com.beamcard.user.auth.exception;

public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String username) {
        super("username already taken: " + username);
    }
}
