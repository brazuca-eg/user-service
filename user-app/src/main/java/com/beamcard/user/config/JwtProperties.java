package com.beamcard.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("beamcard.auth.jwt")
public record JwtProperties(
        String privateKeyLocation,
        String publicKeyLocation,
        boolean generateIfMissing,
        String privateKeyPem,
        String publicKeyPem) {

    public boolean hasInlinePem() {
        return notBlank(privateKeyPem) && notBlank(publicKeyPem);
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}
