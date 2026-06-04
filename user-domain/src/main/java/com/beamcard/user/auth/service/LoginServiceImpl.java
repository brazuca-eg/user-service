package com.beamcard.user.auth.service;

import com.beamcard.user.auth.exception.AccountNotActiveException;
import com.beamcard.user.auth.exception.InvalidCredentialsException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;
    private final UsernameRepository usernameRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public LoginResult login(LoginCommand command) {
        User user = userRepository.findByEmail(command.email()).orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(command.rawPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountNotActiveException();
        }

        String username = usernameRepository
                .findUsernameByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException("user %s has no username".formatted(user.getId())));

        JwtService.IssuedToken token = jwtService.issueAccessToken(user, username);
        log.info("Login succeeded for user {} ({})", user.getId(), user.getEmail());

        return new LoginResult(user, username, token);
    }
}
