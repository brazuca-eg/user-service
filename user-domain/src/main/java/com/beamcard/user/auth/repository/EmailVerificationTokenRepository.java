package com.beamcard.user.auth.repository;

import com.beamcard.user.auth.model.EmailVerificationToken;
import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationTokenRepository {

    EmailVerificationToken save(EmailVerificationToken token);

    Optional<EmailVerificationToken> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);
}
