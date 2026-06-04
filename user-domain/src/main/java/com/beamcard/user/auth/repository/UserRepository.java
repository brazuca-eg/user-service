package com.beamcard.user.auth.repository;

import com.beamcard.user.auth.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    Optional<User> findById(UUID id);

    User save(User user);
}
