package com.beamcard.user.auth.rest.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.rest.config.SecurityConfig;
import com.beamcard.user.auth.service.AccountService;
import com.beamcard.user.auth.service.JwtService;
import com.beamcard.user.auth.service.JwtService.AuthenticatedUser;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AccountService accountService;

    @MockBean
    JwtService jwtService;

    @Test
    void me_returns200_withCurrentUser_whenTokenValid() throws Exception {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        User user = User.builder()
                .id(id)
                .email("alice@example.com")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .createdAt(createdAt)
                .build();

        when(jwtService.verify("validtoken")).thenReturn(new AuthenticatedUser(id, "alice", "free"));
        when(accountService.getById(id)).thenReturn(new AccountService.AccountView(user, "alice"));

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer validtoken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.plan").value("free"))
                .andExpect(jsonPath("$.created_at").exists());
    }

    @Test
    void me_returns401_whenNoToken() throws Exception {
        mockMvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void me_returns401_whenTokenInvalid() throws Exception {
        when(jwtService.verify("badtoken")).thenThrow(new JwtException("bad"));

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer badtoken"))
                .andExpect(status().isUnauthorized());
    }
}
