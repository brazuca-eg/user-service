package com.beamcard.user.auth.conf;

import com.beamcard.user.auth.model.SigningKey;
import com.beamcard.user.auth.repository.PasswordResetTokenRepository;
import com.beamcard.user.auth.repository.RefreshTokenRepository;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import com.beamcard.user.auth.service.AccountService;
import com.beamcard.user.auth.service.AccountServiceImpl;
import com.beamcard.user.auth.service.EmailSender;
import com.beamcard.user.auth.service.JwtService;
import com.beamcard.user.auth.service.JwtServiceImpl;
import com.beamcard.user.auth.service.LoginService;
import com.beamcard.user.auth.service.LoginServiceImpl;
import com.beamcard.user.auth.service.PasswordResetService;
import com.beamcard.user.auth.service.PasswordResetServiceImpl;
import com.beamcard.user.auth.service.RefreshTokenService;
import com.beamcard.user.auth.service.RefreshTokenServiceImpl;
import com.beamcard.user.auth.service.SignupService;
import com.beamcard.user.auth.service.SignupServiceImpl;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
public class DomainConfig {

    @Bean
    public JwtService jwtService(
            SigningKey signingKey, @Value("${beamcard.auth.access-token-ttl}") Duration accessTokenTtl) {
        return new JwtServiceImpl(signingKey, accessTokenTtl);
    }

    @Bean
    public RefreshTokenService refreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            UsernameRepository usernameRepository,
            JwtService jwtService,
            @Value("${beamcard.auth.refresh-token-ttl}") Duration refreshTokenTtl) {
        return new RefreshTokenServiceImpl(
                refreshTokenRepository, userRepository, usernameRepository, jwtService, refreshTokenTtl);
    }

    @Bean
    public SignupService signupService(
            UserRepository userRepository,
            UsernameRepository usernameRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        return new SignupServiceImpl(userRepository, usernameRepository, passwordEncoder, jwtService, refreshTokenService);
    }

    @Bean
    public LoginService loginService(
            UserRepository userRepository,
            UsernameRepository usernameRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService) {
        return new LoginServiceImpl(userRepository, usernameRepository, passwordEncoder, jwtService, refreshTokenService);
    }

    @Bean
    public AccountService accountService(UserRepository userRepository, UsernameRepository usernameRepository) {
        return new AccountServiceImpl(userRepository, usernameRepository);
    }

    @Bean
    public PasswordResetService passwordResetService(
            UserRepository userRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            EmailSender emailSender,
            @Value("${beamcard.auth.password-reset.token-ttl}") Duration tokenTtl,
            @Value("${beamcard.auth.password-reset.reset-url-template}") String resetUrlTemplate) {
        return new PasswordResetServiceImpl(
                userRepository,
                passwordResetTokenRepository,
                refreshTokenRepository,
                passwordEncoder,
                emailSender,
                tokenTtl,
                resetUrlTemplate);
    }
}
