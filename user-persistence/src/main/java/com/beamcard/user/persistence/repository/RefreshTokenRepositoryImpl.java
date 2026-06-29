package com.beamcard.user.persistence.repository;

import com.beamcard.user.auth.model.RefreshToken;
import com.beamcard.user.auth.repository.RefreshTokenRepository;
import com.beamcard.user.persistence.mapper.RefreshTokenPersistenceMapper;
import com.beamcard.user.persistence.model.RefreshTokenJpa;
import com.beamcard.user.persistence.repository.jpa.RefreshTokenJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;
    private final RefreshTokenPersistenceMapper mapper;

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpa saved = jpaRepository.save(mapper.toJpa(token));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void revokeAllForUser(UUID userId) {
        jpaRepository.revokeAllActiveForUser(userId, Instant.now());
    }
}
