package com.beamcard.user.auth.exception;

public class InvalidVerificationTokenException extends RuntimeException {

    public InvalidVerificationTokenException() {
        super("email verification token is invalid, expired, or already used");
    }
}
