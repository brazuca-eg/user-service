package com.beamcard.user.auth.rest.controller;

import com.beamcard.user.auth.service.AccountService;
import com.beamcard.user.auth.service.AccountService.AccountView;
import com.beamcard.user.client.UserSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final AccountService accountService;

    @GetMapping("/by-username/{username}")
    public UserSummary getByUsername(@PathVariable String username) {
        AccountView account = accountService.getByUsername(username);
        return new UserSummary(
                account.user().getId(),
                account.username(),
                account.user().getPlan().name().toLowerCase(),
                account.user().getStatus().name().toLowerCase());
    }
}
