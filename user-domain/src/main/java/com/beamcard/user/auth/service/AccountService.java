package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;
import java.util.UUID;

public interface AccountService {

    AccountView getById(UUID userId);

    AccountView getByUsername(String username);

    AccountUpdateResult updateAccount(UUID userId, String newUsername, String newLocale);

    record AccountView(User user, String username) {}

    record AccountUpdateResult(User user, String username, JwtService.IssuedToken token, String refreshToken) {}
}
