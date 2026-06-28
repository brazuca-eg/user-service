package com.beamcard.user.persistence.repository.jpa;

import com.beamcard.user.persistence.model.PasswordResetTokenJpa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpa, UUID> {

    Optional<PasswordResetTokenJpa> findByTokenHash(String tokenHash);

    void deleteByUserId(UUID userId);
}
