package com.beamcard.user.auth.service;

public interface EmailSender {

    void sendPasswordReset(String toEmail, String resetUrl);

    void sendEmailVerification(String toEmail, String verifyUrl);
}
