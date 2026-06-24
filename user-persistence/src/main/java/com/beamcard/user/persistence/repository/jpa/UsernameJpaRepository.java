package com.beamcard.user.persistence.repository.jpa;

import com.beamcard.user.persistence.model.UsernameJpa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsernameJpaRepository extends JpaRepository<UsernameJpa, String> {

    Optional<UsernameJpa> findByUserId(UUID userId);
}
