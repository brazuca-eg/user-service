package com.beamcard.user.auth.repository;

import com.beamcard.user.auth.model.User;
import java.util.Optional;

public interface UserRepository {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    User save(User user);
}
