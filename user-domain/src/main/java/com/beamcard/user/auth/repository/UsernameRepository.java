package com.beamcard.user.auth.repository;

import java.util.Optional;
import java.util.UUID;

public interface UsernameRepository {

    boolean existsByUsername(String username);

    Optional<String> findUsernameByUserId(UUID userId);

    Optional<UUID> findUserIdByUsername(String username);

    void save(String username, UUID userId);

    void changeUsername(UUID userId, String newUsername);
}
