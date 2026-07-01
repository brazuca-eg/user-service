package com.beamcard.user.auth.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beamcard.user.auth.exception.InvalidRefreshTokenException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.rest.config.SecurityConfig;
import com.beamcard.user.auth.rest.exception.GlobalExceptionHandler;
import com.beamcard.user.auth.service.JwtService;
import com.beamcard.user.auth.service.JwtService.AuthenticatedUser;
import com.beamcard.user.auth.service.RefreshTokenService;
import com.beamcard.user.auth.service.RefreshTokenService.RefreshResult;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RefreshController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class RefreshControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    RefreshTokenService refreshTokenService;

    @MockBean
    JwtService jwtService;

    @Test
    void refresh_returns200_withRotatedTokenPair() throws Exception {
        User user = User.builder()
                .id(UUID.randomUUID())
                .email("alice@example.com")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .build();
        when(refreshTokenService.refresh("old.refresh"))
                .thenReturn(
                        new RefreshResult(user, "alice", new JwtService.IssuedToken("new.jwt", 900), "new.refresh"));

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"old.refresh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("new.jwt"))
                .andExpect(jsonPath("$.refresh_token").value("new.refresh"))
                .andExpect(jsonPath("$.user.username").value("alice"));
    }

    @Test
    void refresh_returns401_whenTokenInvalid() throws Exception {
        when(refreshTokenService.refresh(any())).thenThrow(new InvalidRefreshTokenException());

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_refresh_token"));
    }

    @Test
    void refresh_returns400_whenTokenMissing() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logout_returns204_andRevokes_whenAuthenticated() throws Exception {
        when(jwtService.verify("validtoken")).thenReturn(new AuthenticatedUser(UUID.randomUUID(), "alice", "free"));

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer validtoken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"old.refresh\"}"))
                .andExpect(status().isNoContent());

        verify(refreshTokenService).revoke("old.refresh");
    }

    @Test
    void logout_returns401_withoutToken() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\":\"x\"}"))
                .andExpect(status().isUnauthorized());
    }
}
