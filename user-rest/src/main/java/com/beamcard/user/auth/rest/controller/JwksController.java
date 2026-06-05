package com.beamcard.user.auth.rest.controller;

import com.beamcard.user.auth.model.JwksDocument;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publishes the public signing key as a JWKS document so other services
 * can verify the access tokens user-service issues
 */
@RestController
public class JwksController {

    private final JwksDocument jwksDocument;

    public JwksController(JwksDocument jwksDocument) {
        this.jwksDocument = jwksDocument;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return jwksDocument.content();
    }
}
