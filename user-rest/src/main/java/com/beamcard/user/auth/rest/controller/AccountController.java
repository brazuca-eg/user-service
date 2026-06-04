package com.beamcard.user.auth.rest.controller;

import com.beamcard.user.auth.rest.model.response.AccountResponse;
import com.beamcard.user.auth.service.AccountService;
import com.beamcard.user.auth.service.AccountService.AccountView;
import com.beamcard.user.auth.service.JwtService.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/me")
    public AccountResponse getCurrentAccount(@AuthenticationPrincipal AuthenticatedUser principal) {
        AccountView account = accountService.getById(principal.id());
        return AccountResponse.of(account.user(), account.username());
    }
}
