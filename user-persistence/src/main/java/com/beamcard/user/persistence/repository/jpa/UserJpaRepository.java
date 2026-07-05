package com.beamcard.user.persistence.repository.jpa;

import com.beamcard.user.persistence.model.UserJpa;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpa, UUID> {

    Optional<UserJpa> findByEmail(String email);

    Optional<UserJpa> findByGoogleSub(String googleSub);

    boolean existsByEmail(String email);
}
