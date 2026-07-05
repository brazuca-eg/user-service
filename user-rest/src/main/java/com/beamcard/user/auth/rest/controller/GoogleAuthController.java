package com.beamcard.user.auth.rest.controller;

import com.beamcard.user.auth.rest.model.request.GoogleAuthRequest;
import com.beamcard.user.auth.rest.model.response.AuthResponse;
import com.beamcard.user.auth.service.GoogleAuthService;
import com.beamcard.user.auth.service.GoogleAuthService.GoogleAuthCommand;
import com.beamcard.user.auth.service.GoogleAuthService.GoogleAuthResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Validated
@RequiredArgsConstructor
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    @PostMapping("/oauth/google")
    public AuthResponse signInWithGoogle(@Valid @RequestBody GoogleAuthRequest request) {
        GoogleAuthResult authResult =
                googleAuthService.authenticate(new GoogleAuthCommand(request.idToken(), request.locale()));
        return AuthResponse.of(
                authResult.user(),
                authResult.username(),
                authResult.token(),
                authResult.refreshToken(),
                authResult.needsUsername());
    }
}
