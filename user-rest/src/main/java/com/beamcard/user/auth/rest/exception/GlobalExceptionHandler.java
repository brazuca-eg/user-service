package com.beamcard.user.auth.rest.exception;

import com.beamcard.user.auth.exception.AccountNotActiveException;
import com.beamcard.user.auth.exception.EmailAlreadyExistsException;
import com.beamcard.user.auth.exception.InvalidCredentialsException;
import com.beamcard.user.auth.exception.InvalidRefreshTokenException;
import com.beamcard.user.auth.exception.InvalidResetTokenException;
import com.beamcard.user.auth.exception.UserNotFoundException;
import com.beamcard.user.auth.exception.UsernameAlreadyExistsException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // TODO: `email_taken` is an accepted account-enumeration trade-off.
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

    @ExceptionHandler(InvalidResetTokenException.class)
    ProblemDetail handle(InvalidResetTokenException e) {
        return problem(HttpStatus.BAD_REQUEST, "invalid_reset_token", "This reset link is invalid or has expired.");
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    ProblemDetail handle(InvalidRefreshTokenException e) {
        return problem(HttpStatus.UNAUTHORIZED, "invalid_refresh_token", "Your session has expired. Please sign in again.");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), messageOr(fieldError.getDefaultMessage()));
        }
        log.debug("Request body validation failed: {}", errors);
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed.");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            errors.put(violation.getPropertyPath().toString(), messageOr(violation.getMessage()));
        }
        log.debug("Constraint violation: {}", errors);
        ProblemDetail body = problem(HttpStatus.BAD_REQUEST, "validation_failed", "Request validation failed.");
        body.setProperty("errors", errors);
        return body;
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal_error", "An unexpected error occurred.");
    }

    private static String messageOr(String message) {
        return message == null ? "invalid" : message;
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(status.getReasonPhrase());
        pd.setProperty("code", code);
        return pd;
    }
}
