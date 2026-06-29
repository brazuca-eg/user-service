package com.beamcard.user.persistence.repository.jpa;

import com.beamcard.user.persistence.model.RefreshTokenJpa;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpa, UUID> {

    Optional<RefreshTokenJpa> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenJpa t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    void revokeAllActiveForUser(@Param("userId") UUID userId, @Param("now") Instant now);
}
