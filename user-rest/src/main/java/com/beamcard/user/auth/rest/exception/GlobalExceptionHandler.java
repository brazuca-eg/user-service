package com.beamcard.user.auth.rest.exception;

import com.beamcard.user.auth.exception.EmailAlreadyExistsException;
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
        return conflict("email_taken", "That email is already registered.");
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    ProblemDetail handle(UsernameAlreadyExistsException e) {
        return conflict("username_taken", "That username is already taken.");
    }

    private static ProblemDetail conflict(String slug, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, detail);
        pd.setTitle("Conflict");
        pd.setProperty("code", slug);
        return pd;
    }
}
