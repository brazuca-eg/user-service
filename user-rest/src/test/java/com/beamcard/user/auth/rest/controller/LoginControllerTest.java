package com.beamcard.user.auth.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beamcard.user.auth.exception.AccountNotActiveException;
import com.beamcard.user.auth.exception.InvalidCredentialsException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.rest.config.SecurityConfig;
import com.beamcard.user.auth.rest.exception.GlobalExceptionHandler;
import com.beamcard.user.auth.rest.model.request.LoginRequest;
import com.beamcard.user.auth.service.JwtService;
import com.beamcard.user.auth.service.LoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LoginController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class LoginControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    LoginService loginService;

    @MockBean
    JwtService jwtService;

    @Test
    void login_returns200_withSnakeCaseBody() throws Exception {
        UUID id = UUID.randomUUID();
        User user = User.builder()
                .id(id)
                .email("alice@example.com")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .build();
        when(loginService.login(any()))
                .thenReturn(new LoginService.LoginResult(user, "alice", new JwtService.IssuedToken("jwt.value", 900)));

        String body =
                objectMapper.writeValueAsString(new LoginRequest("alice@example.com", "correcthorsebatterystaple"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("jwt.value"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(jsonPath("$.user.plan").value("free"));
    }

    @Test
    void login_returns401_onInvalidCredentials() throws Exception {
        when(loginService.login(any())).thenThrow(new InvalidCredentialsException());

        String body = objectMapper.writeValueAsString(new LoginRequest("alice@example.com", "wrongpassword1"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("invalid_credentials"));
    }

    @Test
    void login_returns403_whenAccountNotActive() throws Exception {
        when(loginService.login(any())).thenThrow(new AccountNotActiveException());

        String body =
                objectMapper.writeValueAsString(new LoginRequest("alice@example.com", "correcthorsebatterystaple"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("account_inactive"));
    }

    @Test
    void login_returns400_whenEmailMissing() throws Exception {
        String body = """
                { "password": "correcthorsebatterystaple" }
                """;

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
