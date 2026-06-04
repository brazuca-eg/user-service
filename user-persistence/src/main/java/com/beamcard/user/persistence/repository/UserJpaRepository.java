package com.beamcard.user.persistence.repository;

import com.beamcard.user.persistence.model.UserJpa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface UserJpaRepository extends JpaRepository<UserJpa, UUID> {

    Optional<UserJpa> findByEmail(String email);

    boolean existsByEmail(String email);
}
