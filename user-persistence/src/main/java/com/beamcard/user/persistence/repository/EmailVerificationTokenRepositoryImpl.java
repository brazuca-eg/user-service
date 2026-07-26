package com.beamcard.user.persistence.repository;

import com.beamcard.user.auth.model.EmailVerificationToken;
import com.beamcard.user.auth.repository.EmailVerificationTokenRepository;
import com.beamcard.user.persistence.mapper.EmailVerificationTokenPersistenceMapper;
import com.beamcard.user.persistence.model.EmailVerificationTokenJpa;
import com.beamcard.user.persistence.repository.jpa.EmailVerificationTokenJpaRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class EmailVerificationTokenRepositoryImpl implements EmailVerificationTokenRepository {

    private final EmailVerificationTokenJpaRepository jpaRepository;
    private final EmailVerificationTokenPersistenceMapper mapper;

    @Override
    public EmailVerificationToken save(EmailVerificationToken token) {
        EmailVerificationTokenJpa saved = jpaRepository.save(mapper.toJpa(token));
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<EmailVerificationToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }
}
