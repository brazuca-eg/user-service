package com.beamcard.user.auth.exception;

public class EmailNotVerifiedException extends RuntimeException {

    public EmailNotVerifiedException() {
        super("email address is not verified");
    }
}
