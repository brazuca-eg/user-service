package com.beamcard.user.auth.exception;

/** Raised when a password change is requested for an account that has no password (Google-only). */
public class PasswordNotSetException extends RuntimeException {

    public PasswordNotSetException() {
        super("this account has no password set");
    }
}
