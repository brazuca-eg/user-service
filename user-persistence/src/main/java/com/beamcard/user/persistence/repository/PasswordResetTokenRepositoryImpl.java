package com.beamcard.user.persistence.repository;

import com.beamcard.user.auth.model.PasswordResetToken;
import com.beamcard.user.auth.repository.PasswordResetTokenRepository;
import com.beamcard.user.persistence.mapper.PasswordResetTokenPersistenceMapper;
import com.beamcard.user.persistence.model.PasswordResetTokenJpa;
import com.beamcard.user.persistence.repository.jpa.PasswordResetTokenJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordResetTokenRepositoryImpl implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepository;
    private final PasswordResetTokenPersistenceMapper mapper;

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpa saved = jpaRepository.save(mapper.toJpa(token));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
