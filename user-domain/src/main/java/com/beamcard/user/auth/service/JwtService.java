package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;

public interface JwtService {

    IssuedToken issueAccessToken(User user, String username);

    record IssuedToken(String value, long expiresInSeconds) {}
}
