package com.beamcard.user.oauth;

import com.beamcard.user.auth.exception.InvalidGoogleTokenException;
import com.beamcard.user.auth.service.GoogleIdentityVerifier;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleIdentityVerifierImpl implements GoogleIdentityVerifier {

    private final GoogleIdTokenVerifier verifier;

    public GoogleIdentityVerifierImpl(String clientId) {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(List.of(clientId))
                .build();
    }

    @Override
    public GoogleIdentity verify(String idToken) {
        GoogleIdToken token;
        try {
            token = verifier.verify(idToken);
        } catch (GeneralSecurityException | IOException e) {
            log.warn("Google ID token verification failed", e);
            throw new InvalidGoogleTokenException("Could not verify Google ID token");
        }
        if (token == null) {
            throw new InvalidGoogleTokenException("Invalid Google ID token");
        }
        Payload payload = token.getPayload();
        return new GoogleIdentity(
                payload.getSubject(), payload.getEmail(), Boolean.TRUE.equals(payload.getEmailVerified()));
    }
}
