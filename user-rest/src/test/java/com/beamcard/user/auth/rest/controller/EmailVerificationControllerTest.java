package com.beamcard.user.auth.rest.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.beamcard.user.auth.exception.InvalidVerificationTokenException;
import com.beamcard.user.auth.rest.config.SecurityConfig;
import com.beamcard.user.auth.rest.exception.GlobalExceptionHandler;
import com.beamcard.user.auth.service.EmailVerificationService;
import com.beamcard.user.auth.service.JwtService;
import com.beamcard.user.auth.service.JwtService.AuthenticatedUser;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmailVerificationController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class EmailVerificationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    EmailVerificationService emailVerificationService;

    @MockBean
    JwtService jwtService;

    @Test
    void confirm_returns204_andVerifies() throws Exception {
        mockMvc.perform(post("/auth/email/verify/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"the-token\"}"))
                .andExpect(status().isNoContent());
        verify(emailVerificationService).confirm("the-token");
    }

    @Test
    void confirm_returns400_onBlankToken() throws Exception {
        mockMvc.perform(post("/auth/email/verify/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void confirm_returns400_problemCode_onInvalidToken() throws Exception {
        doThrow(new InvalidVerificationTokenException())
                .when(emailVerificationService)
                .confirm("bad");

        mockMvc.perform(post("/auth/email/verify/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("invalid_verification_token"));
    }

    @Test
    void resendByEmail_isPublic_andReturns202() throws Exception {
        mockMvc.perform(post("/auth/email/verify/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\"}"))
                .andExpect(status().isAccepted());
        verify(emailVerificationService).resendByEmail("alice@example.com");
    }

    @Test
    void resend_returns401_withoutToken() throws Exception {
        mockMvc.perform(post("/auth/email/verify/request")).andExpect(status().isUnauthorized());
    }

    @Test
    void resend_returns202_withToken() throws Exception {
        UUID userId = UUID.randomUUID();
        when(jwtService.verify("validtoken")).thenReturn(new AuthenticatedUser(userId, "alice", "free"));

        mockMvc.perform(post("/auth/email/verify/request").header("Authorization", "Bearer validtoken"))
                .andExpect(status().isAccepted());
        verify(emailVerificationService).resend(userId);
    }
}
