package com.beamcard.user.auth.rest.exception;

import com.beamcard.user.auth.exception.AccountNotActiveException;
import com.beamcard.user.auth.exception.EmailAlreadyExistsException;
import com.beamcard.user.auth.exception.InvalidCredentialsException;
import com.beamcard.user.auth.exception.UserNotFoundException;
import com.beamcard.user.auth.exception.UsernameAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // TODO: `email_taken` is an accepted account-enumeration trade-off — it
    @ExceptionHandler(EmailAlreadyExistsException.class)
    ProblemDetail handle(EmailAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, "email_taken", "That email is already registered.");
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ProblemDetail handle(UsernameAlreadyExistsException e) {
        return problem(HttpStatus.CONFLICT, "username_taken", "That username is already taken.");
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail handle(InvalidCredentialsException e) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid_credentials", "Invalid email or password.");
    }

    @ExceptionHandler(AccountNotActiveException.class)
    ProblemDetail handle(AccountNotActiveException e) {
        return problem(HttpStatus.FORBIDDEN, "account_inactive", "This account is not active.");
    }

    @ExceptionHandler(UserNotFoundException.class)
    ProblemDetail handle(UserNotFoundException e) {
        return problem(HttpStatus.NOT_FOUND, "user_not_found", "User not found.");
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        return pd;
    }
}
