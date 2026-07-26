package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;
import java.util.UUID;

public interface EmailVerificationService {

    void sendVerification(User user);

    void resend(UUID userId);

    void resendByEmail(String email);

    void confirm(String rawToken);
}
