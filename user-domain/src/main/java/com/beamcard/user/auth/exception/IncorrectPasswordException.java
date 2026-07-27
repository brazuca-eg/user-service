package com.beamcard.user.auth.exception;

public class IncorrectPasswordException extends RuntimeException {

    public IncorrectPasswordException() {
        super("current password is incorrect");
    }
}
