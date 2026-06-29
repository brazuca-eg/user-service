package com.beamcard.user.auth.rest.controller;

import com.beamcard.user.auth.rest.model.request.LoginRequest;
import com.beamcard.user.auth.rest.model.response.AuthResponse;
import com.beamcard.user.auth.service.LoginService;
import com.beamcard.user.auth.service.LoginService.LoginCommand;
import com.beamcard.user.auth.service.LoginService.LoginResult;
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
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginService.login(new LoginCommand(request.email(), request.password()));
        return AuthResponse.of(result.user(), result.username(), result.token(), result.refreshToken());
    }
}
