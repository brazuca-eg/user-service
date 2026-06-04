package com.beamcard.user.auth.service;

import com.beamcard.user.auth.exception.EmailAlreadyExistsException;
import com.beamcard.user.auth.exception.UsernameAlreadyExistsException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignupServiceImpl implements SignupService {

    private final UserRepository userRepository;
    private final UsernameRepository usernameRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public SignupResult signup(SignupCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyExistsException(command.email());
        }
        if (usernameRepository.existsByUsername(command.username())) {
            throw new UsernameAlreadyExistsException(command.username());
        }

        User toPersist = User.builder()
                .email(command.email())
                .passwordHash(passwordEncoder.encode(command.rawPassword()))
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .build();

        User saved = userRepository.save(toPersist);
        usernameRepository.save(command.username(), saved.getId());

        JwtService.IssuedToken token = jwtService.issueAccessToken(saved, command.username());
        log.info("Signup succeeded for user {} ({})", saved.getId(), command.email());

        return new SignupResult(saved, command.username(), token);
    }
}
