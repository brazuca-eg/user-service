package com.beamcard.user.auth.rest.controller;

import com.beamcard.user.auth.rest.model.request.LogoutRequest;
import com.beamcard.user.auth.rest.model.request.RefreshRequest;
import com.beamcard.user.auth.rest.model.response.AuthResponse;
import com.beamcard.user.auth.service.RefreshTokenService;
import com.beamcard.user.auth.service.RefreshTokenService.RefreshResult;
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
public class RefreshController {

    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshResult result = refreshTokenService.refresh(request.refreshToken());
        return AuthResponse.of(result.user(), result.username(), result.accessToken(), result.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokenService.revoke(request.refreshToken());
    }
}
