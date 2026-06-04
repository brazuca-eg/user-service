package com.beamcard.user.auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("email already registered: " + email);
    }
}
