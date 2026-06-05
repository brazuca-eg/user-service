package com.beamcard.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("beamcard.auth.jwt")
public record JwtProperties(String privateKeyLocation, String publicKeyLocation, boolean generateIfMissing) {}
