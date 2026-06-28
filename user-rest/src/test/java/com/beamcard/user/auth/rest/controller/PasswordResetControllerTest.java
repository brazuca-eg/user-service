package com.beamcard.user.auth.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beamcard.user.auth.exception.InvalidResetTokenException;
import com.beamcard.user.auth.rest.config.SecurityConfig;
import com.beamcard.user.auth.rest.exception.GlobalExceptionHandler;
import com.beamcard.user.auth.rest.model.request.ForgotPasswordRequest;
import com.beamcard.user.auth.rest.model.request.ResetPasswordRequest;
import com.beamcard.user.auth.service.JwtService;
import com.beamcard.user.auth.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PasswordResetController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class PasswordResetControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    PasswordResetService passwordResetService;

    @MockBean
    JwtService jwtService;

    @Test
    void forgot_returns202_andDelegates() throws Exception {
        String body = objectMapper.writeValueAsString(new ForgotPasswordRequest("alice@example.com"));

        mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isAccepted());

        verify(passwordResetService).requestReset("alice@example.com");
    }

    @Test
    void forgot_returns400_onInvalidEmail() throws Exception {
        String body = objectMapper.writeValueAsString(new ForgotPasswordRequest("not-an-email"));

        mockMvc.perform(post("/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reset_returns204_andDelegates() throws Exception {
        String body = objectMapper.writeValueAsString(new ResetPasswordRequest("the-token", "brandnewpassword"));

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        verify(passwordResetService).resetPassword("the-token", "brandnewpassword");
    }

    @Test
    void reset_returns400_onShortPassword() throws Exception {
        String body = objectMapper.writeValueAsString(new ResetPasswordRequest("the-token", "short"));

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reset_returns400_withCode_whenTokenInvalid() throws Exception {
        doThrow(new InvalidResetTokenException()).when(passwordResetService).resetPassword(any(), any());
        String body = objectMapper.writeValueAsString(new ResetPasswordRequest("bad-token", "brandnewpassword"));

        mockMvc.perform(post("/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_reset_token"));
    }
}
