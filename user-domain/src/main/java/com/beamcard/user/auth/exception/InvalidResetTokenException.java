package com.beamcard.user.auth.exception;

public class InvalidResetTokenException extends RuntimeException {

    public InvalidResetTokenException() {
        super("password reset token is invalid, expired, or already used");
    }
}
