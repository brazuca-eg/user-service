package com.beamcard.user.persistence;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan("com.beamcard.user.persistence.model")
@EnableJpaRepositories("com.beamcard.user.persistence.repository")
class PersistenceTestConfig {}
