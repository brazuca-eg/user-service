package com.beamcard.user.persistence.repository.jpa;

import com.beamcard.user.persistence.model.EmailVerificationTokenJpa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenJpaRepository extends JpaRepository<EmailVerificationTokenJpa, UUID> {

    Optional<EmailVerificationTokenJpa> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);
}
