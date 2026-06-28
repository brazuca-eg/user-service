package com.beamcard.user.persistence.config;

import com.beamcard.user.auth.repository.PasswordResetTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import com.beamcard.user.persistence.mapper.PasswordResetTokenPersistenceMapper;
import com.beamcard.user.persistence.mapper.UserPersistenceMapper;
import com.beamcard.user.persistence.repository.PasswordResetTokenRepositoryImpl;
import com.beamcard.user.persistence.repository.UserRepositoryImpl;
import com.beamcard.user.persistence.repository.UsernameRepositoryImpl;
import com.beamcard.user.persistence.repository.jpa.PasswordResetTokenJpaRepository;
import com.beamcard.user.persistence.repository.jpa.UserJpaRepository;
import com.beamcard.user.persistence.repository.jpa.UsernameJpaRepository;
import org.mapstruct.factory.Mappers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(basePackages = "com.beamcard.user.persistence.repository.jpa")
public class PersistenceConfig {

    @Bean
    public UserPersistenceMapper userPersistenceMapper() {
        return Mappers.getMapper(UserPersistenceMapper.class);
    }

    @Bean
    public UserRepository userRepository(
            UserJpaRepository userJpaRepository, UserPersistenceMapper userPersistenceMapper) {
        return new UserRepositoryImpl(userJpaRepository, userPersistenceMapper);
    }

    @Bean
    public UsernameRepository usernameRepository(UsernameJpaRepository usernameJpaRepository) {
        return new UsernameRepositoryImpl(usernameJpaRepository);
    }

    @Bean
    public PasswordResetTokenPersistenceMapper passwordResetTokenPersistenceMapper() {
        return Mappers.getMapper(PasswordResetTokenPersistenceMapper.class);
    }

    @Bean
    public PasswordResetTokenRepository passwordResetTokenRepository(
            PasswordResetTokenJpaRepository jpaRepository, PasswordResetTokenPersistenceMapper mapper) {
        return new PasswordResetTokenRepositoryImpl(jpaRepository, mapper);
    }
}
