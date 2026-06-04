package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;

public interface SignupService {

    SignupResult signup(SignupCommand command);

    record SignupCommand(String email, String rawPassword, String username) {}

    record SignupResult(User user, String username, JwtService.IssuedToken token) {}
}
