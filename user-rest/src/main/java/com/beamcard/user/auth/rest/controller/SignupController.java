package com.beamcard.user.auth.rest.controller;

import com.beamcard.user.auth.rest.model.request.SignupRequest;
import com.beamcard.user.auth.rest.model.response.AuthResponse;
import com.beamcard.user.auth.service.SignupService;
import com.beamcard.user.auth.service.SignupService.SignupCommand;
import com.beamcard.user.auth.service.SignupService.SignupResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Validated
@RequiredArgsConstructor
public class SignupController {

    private final SignupService signupService;

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse signup(@Valid @RequestBody SignupRequest request) {
        SignupResult result =
                signupService.signup(new SignupCommand(request.email(), request.password(), request.username()));

        return AuthResponse.of(result.user(), result.username(), result.token(), result.refreshToken());
    }
}
