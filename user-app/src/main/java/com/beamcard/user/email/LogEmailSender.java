package com.beamcard.user.email;

import com.beamcard.user.auth.service.EmailSender;
import lombok.extern.slf4j.Slf4j;

/**
 * Dev only
 */
@Slf4j
public class LogEmailSender implements EmailSender {

    @Override
    public void sendPasswordReset(String toEmail, String resetUrl) {
        log.info("[email:log] Password reset for {}\n  reset link: {}", toEmail, resetUrl);
    }
}
