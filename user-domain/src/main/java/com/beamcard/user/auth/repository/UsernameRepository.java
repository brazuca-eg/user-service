package com.beamcard.user.auth.repository;

import java.util.UUID;

public interface UsernameRepository {

    boolean existsByUsername(String username);

    void save(String username, UUID userId);
}
