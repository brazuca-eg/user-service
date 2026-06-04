package com.beamcard.user.auth.exception;

public class AccountNotActiveException extends RuntimeException {

    public AccountNotActiveException() {
        super("account is not active");
    }
}
