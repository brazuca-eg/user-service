package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;

public interface LoginService {

    LoginResult login(LoginCommand command);

    record LoginCommand(String email, String rawPassword) {}

    record LoginResult(User user, String username, JwtService.IssuedToken token, String refreshToken) {}
}
