package com.beamcard.user.config;

import com.beamcard.user.auth.model.JwksDocument;
import com.beamcard.user.auth.model.SigningKey;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtKeyConfig {

    @Bean
    public SigningKey jwtSigningKey(JwtProperties properties) {
        KeyPair keyPair = new JwtKeyLoader(properties).load();
        return new SigningKey(keyPair, toRsaKey(keyPair).getKeyID());
    }

    @Bean
    public JwksDocument jwksDocument(SigningKey signingKey) {
        return new JwksDocument(new JWKSet(toRsaKey(signingKey.keyPair()).toPublicJWK()).toJSONObject());
    }

    private static RSAKey toRsaKey(KeyPair keyPair) {
        try {
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyUse(KeyUse.SIGNATURE)
                    .algorithm(JWSAlgorithm.RS256)
                    .keyIDFromThumbprint()
                    .build();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to derive JWK from the signing keypair", e);
        }
    }
}
