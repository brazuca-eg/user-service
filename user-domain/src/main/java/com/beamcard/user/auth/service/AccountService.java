package com.beamcard.user.auth.service;

import com.beamcard.user.auth.model.User;
import java.util.UUID;

public interface AccountService {

    AccountView getById(UUID userId);

    AccountView getByUsername(String username);

    record AccountView(User user, String username) {}
}
