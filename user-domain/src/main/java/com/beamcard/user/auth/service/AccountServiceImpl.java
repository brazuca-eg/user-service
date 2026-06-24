package com.beamcard.user.auth.service;

import com.beamcard.user.auth.exception.UserNotFoundException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.repository.UserRepository;
import com.beamcard.user.auth.repository.UsernameRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final UserRepository userRepository;
    private final UsernameRepository usernameRepository;

    @Override
    public AccountView getById(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        String username = usernameRepository
                .findUsernameByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("user %s has no username".formatted(userId)));
        return new AccountView(user, username);
    }

    @Override
    public AccountView getByUsername(String username) {
        UUID userId = usernameRepository
                .findUserIdByUsername(username)
                .orElseThrow(() -> new UserNotFoundException(username));
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return new AccountView(user, username);
    }
}
