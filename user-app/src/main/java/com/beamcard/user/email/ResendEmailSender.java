package com.beamcard.user.email;

import com.beamcard.user.auth.service.EmailSender;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@Slf4j
public class ResendEmailSender implements EmailSender {

    private final RestClient client;
    private final String from;

    public ResendEmailSender(String apiKey, String baseUrl, String from) {
        this.from = from;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    @Override
    public void sendPasswordReset(String toEmail, String resetUrl) {
        Map<String, Object> body = Map.of(
                "from",
                from,
                "to",
                List.of(toEmail),
                "subject",
                "Reset your Beamcard password",
                "html",
                html(resetUrl));

        client.post()
                .uri("/emails")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        log.info("Password reset email sent via Resend to {}.", toEmail);
    }

    private static String html(String resetUrl) {
        return """
                <p>We received a request to reset your Beamcard password.</p>
                <p><a href="%s">Choose a new password</a></p>
                <p>This link expires shortly and can be used once. If you didn't request
                this, you can safely ignore this email.</p>
                """
                .formatted(resetUrl);
    }
}
