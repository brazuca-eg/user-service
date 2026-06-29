package com.beamcard.user.auth.exception;

public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("refresh token is invalid, expired, or revoked");
    }
}
