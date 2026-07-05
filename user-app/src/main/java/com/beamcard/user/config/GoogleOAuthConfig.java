package com.beamcard.user.config;

import com.beamcard.user.auth.service.GoogleIdentityVerifier;
import com.beamcard.user.oauth.GoogleIdentityVerifierImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

@Slf4j
@Configuration
public class GoogleOAuthConfig {

    @Bean
    public GoogleIdentityVerifier googleIdentityVerifier(@Value("${beamcard.auth.google.client-id:}") String clientId) {
        if (!StringUtils.hasText(clientId)) {
            log.warn("GOOGLE_CLIENT_ID is not set — Google sign-in will reject every token (401 invalid_google_token). "
                    + "Set it to the OAuth Web Client ID to enable Sign in with Google.");
        }
        return new GoogleIdentityVerifierImpl(clientId);
    }
}
