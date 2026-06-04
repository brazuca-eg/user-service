package com.beamcard.user.persistence.repository;

import com.beamcard.user.persistence.model.UsernameJpa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UsernameJpaRepository extends JpaRepository<UsernameJpa, String> {

    Optional<UsernameJpa> findByUserId(UUID userId);
}
