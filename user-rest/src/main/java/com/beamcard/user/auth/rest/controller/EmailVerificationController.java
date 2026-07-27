package com.beamcard.user.auth.rest.controller;

import com.beamcard.user.auth.rest.model.request.ResendVerificationRequest;
import com.beamcard.user.auth.rest.model.request.VerifyEmailRequest;
import com.beamcard.user.auth.service.EmailVerificationService;
import com.beamcard.user.auth.service.JwtService.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/email")
@Validated
@RequiredArgsConstructor
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    @PostMapping("/verify/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.confirm(request.token());
    }

    @PostMapping("/verify/request")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resend(@AuthenticationPrincipal AuthenticatedUser principal) {
        emailVerificationService.resend(principal.id());
    }

    @PostMapping("/verify/resend")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resendByEmail(@Valid @RequestBody ResendVerificationRequest request) {
        emailVerificationService.resendByEmail(request.email());
    }
}
