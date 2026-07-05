package com.beamcard.user.persistence.repository;

import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.persistence.mapper.UserPersistenceMapper;
import com.beamcard.user.persistence.model.UserJpa;
import com.beamcard.user.persistence.repository.jpa.UserJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJpaRepository jpaRepository;
    private final UserPersistenceMapper mapper;

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepository.existsByEmail(email);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepository.findByEmail(email).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByGoogleSub(String googleSub) {
        return jpaRepository.findByGoogleSub(googleSub).map(mapper::toDomain);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public User save(User user) {
        UserJpa saved = jpaRepository.save(mapper.toJpa(user));
        return mapper.toDomain(saved);
    }
}
