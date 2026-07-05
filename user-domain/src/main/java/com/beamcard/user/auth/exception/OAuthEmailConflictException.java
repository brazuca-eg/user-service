package com.beamcard.user.auth.exception;

public class OAuthEmailConflictException extends RuntimeException {

    public OAuthEmailConflictException(String email) {
        super("Email %s is already registered with a password manual login".formatted(email));
    }
}
