package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;

public interface GoogleAuthService {

    GoogleAuthResult authenticate(GoogleAuthCommand command);

    record GoogleAuthCommand(String idToken, String fallbackLocale) {}

    /**
     * @param needsUsername true when just created the account with a placeholder handle
     */
    record GoogleAuthResult(
            User user, String username, JwtService.IssuedToken token, String refreshToken, boolean needsUsername) {}
}
