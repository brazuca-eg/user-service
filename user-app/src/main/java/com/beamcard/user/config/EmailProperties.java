package com.beamcard.user.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("beamcard.email")
public record EmailProperties(String provider, String from, Resend resend) {

    public record Resend(String apiKey, String baseUrl) {}
}
