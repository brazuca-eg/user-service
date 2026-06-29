package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;
import java.util.UUID;

public interface RefreshTokenService {

    String issueRefreshToken(UUID userId);

    RefreshResult refresh(String rawRefreshToken);

    void revoke(String rawRefreshToken);

    record RefreshResult(User user, String username, JwtService.IssuedToken accessToken, String refreshToken) {}
}
