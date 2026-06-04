package com.beamcard.user.persistence.repository;

import com.beamcard.user.persistence.model.UsernameJpa;
import org.springframework.data.jpa.repository.JpaRepository;

interface UsernameJpaRepository extends JpaRepository<UsernameJpa, String> {}
