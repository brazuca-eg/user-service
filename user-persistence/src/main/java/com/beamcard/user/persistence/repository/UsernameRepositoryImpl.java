package com.beamcard.user.persistence.repository;

import com.beamcard.user.auth.repository.UsernameRepository;
import com.beamcard.user.persistence.model.UsernameJpa;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class UsernameRepositoryImpl implements UsernameRepository {

    private final UsernameJpaRepository jpaRepository;

    @Override
    public boolean existsByUsername(String username) {
        return jpaRepository.existsById(username);
    }

    @Override
    public void save(String username, UUID userId) {
        jpaRepository.save(
                UsernameJpa.builder().username(username).userId(userId).build());
    }
}
