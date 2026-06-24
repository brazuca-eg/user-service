package com.beamcard.user.persistence.repository;

import com.beamcard.user.auth.repository.UsernameRepository;
import com.beamcard.user.persistence.model.UsernameJpa;
import com.beamcard.user.persistence.repository.jpa.UsernameJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UsernameRepositoryImpl implements UsernameRepository {

    private final UsernameJpaRepository jpaRepository;

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsById(username);
    }

    @Override
    public Optional<String> findUsernameByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).map(UsernameJpa::getUsername);
    }

    @Override
    public Optional<UUID> findUserIdByUsername(String username) {
        return jpaRepository.findById(username).map(UsernameJpa::getUserId);
    }

    @Override
    public void save(String username, UUID userId) {
        jpaRepository.save(
                UsernameJpa.builder().username(username).userId(userId).build());
    }
}
