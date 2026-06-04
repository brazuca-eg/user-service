package com.beamcard.user.auth.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beamcard.user.auth.exception.EmailAlreadyExistsException;
import com.beamcard.user.auth.exception.UsernameAlreadyExistsException;
import com.beamcard.user.auth.model.User;
import com.beamcard.user.auth.model.UserStatus;
import com.beamcard.user.auth.model.UserSubscriptionPlan;
import com.beamcard.user.auth.rest.config.SecurityConfig;
import com.beamcard.user.auth.rest.exception.GlobalExceptionHandler;
import com.beamcard.user.auth.rest.model.request.SignupRequest;
import com.beamcard.user.auth.service.JwtService;
import com.beamcard.user.auth.service.SignupService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SignupController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class SignupControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    SignupService signupService;

    @MockBean
    JwtService jwtService;

    @Test
    void signup_returns201_withSnakeCaseBody() throws Exception {
        UUID id = UUID.randomUUID();
        User domainUser = User.builder()
                .id(id)
                .email("alice@example.com")
                .plan(UserSubscriptionPlan.FREE)
                .status(UserStatus.ACTIVE)
                .build();

        when(signupService.signup(any()))
                .thenReturn(new SignupService.SignupResult(
                        domainUser, "alice", new JwtService.IssuedToken("jwt.value", 900)));

        String body = objectMapper.writeValueAsString(
                new SignupRequest("alice@example.com", "correcthorsebatterystaple", "alice"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token").value("jwt.value"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(900))
                .andExpect(jsonPath("$.user.id").value(id.toString()))
                .andExpect(jsonPath("$.user.email").value("alice@example.com"))
                .andExpect(jsonPath("$.user.username").value("alice"))
                .andExpect(jsonPath("$.user.plan").value("free"));
    }

    @Test
    void signup_returns400_whenEmailMissing() throws Exception {
        String body =
                """
                { "password": "correcthorsebatterystaple", "username": "alice" }
                """;
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_returns400_whenPasswordTooShort() throws Exception {
        String body =
                """
                { "email": "alice@example.com", "password": "short", "username": "alice" }
                """;
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signup_returns409_whenEmailTaken() throws Exception {
        when(signupService.signup(any())).thenThrow(new EmailAlreadyExistsException("alice@example.com"));

        String body = objectMapper.writeValueAsString(
                new SignupRequest("alice@example.com", "correcthorsebatterystaple", "alice"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("email_taken"));
    }

    @Test
    void signup_returns409_whenUsernameTaken() throws Exception {
        when(signupService.signup(any())).thenThrow(new UsernameAlreadyExistsException("alice"));

        String body = objectMapper.writeValueAsString(
                new SignupRequest("alice@example.com", "correcthorsebatterystaple", "alice"));

        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("username_taken"));
    }
}
