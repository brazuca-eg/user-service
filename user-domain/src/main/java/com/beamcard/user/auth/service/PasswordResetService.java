package com.beamcard.user.auth.service;

public interface PasswordResetService {

    void requestReset(String email);

    void resetPassword(String rawToken, String newRawPassword);
}
