package com.beamcard.user.auth.repository;

import java.util.Optional;
import java.util.UUID;

public interface UsernameRepository {

    boolean existsByUsername(String username);

    Optional<String> findUsernameByUserId(UUID userId);

    void save(String username, UUID userId);
}
