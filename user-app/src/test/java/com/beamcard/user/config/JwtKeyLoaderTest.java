package com.beamcard.user.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtKeyLoaderTest {

    private static KeyPair rsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        return generator.generateKeyPair();
    }

    private static String pem(String type, byte[] der) {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        return "-----BEGIN %s-----\n%s\n-----END %s-----\n".formatted(type, body, type);
    }

    @Test
    void load_readsInlinePemFromProperties() throws Exception {
        KeyPair original = rsaKeyPair();
        String privatePem = pem("PRIVATE KEY", original.getPrivate().getEncoded());
        String publicPem = pem("PUBLIC KEY", original.getPublic().getEncoded());

        JwtProperties properties = new JwtProperties(null, null, false, privatePem, publicPem);
        KeyPair loaded = new JwtKeyLoader(properties).load();

        assertThat(loaded.getPrivate()).isNotNull();
        assertThat(((RSAPublicKey) loaded.getPublic()).getModulus())
                .isEqualTo(((RSAPublicKey) original.getPublic()).getModulus());
    }

    @Test
    void load_toleratesEscapedNewlinesInInlinePem() throws Exception {
        KeyPair original = rsaKeyPair();
        String privatePem =
                pem("PRIVATE KEY", original.getPrivate().getEncoded()).replace("\n", "\\n");
        String publicPem = pem("PUBLIC KEY", original.getPublic().getEncoded()).replace("\n", "\\n");

        JwtProperties properties = new JwtProperties(null, null, false, privatePem, publicPem);
        KeyPair loaded = new JwtKeyLoader(properties).load();

        assertThat(((RSAPublicKey) loaded.getPublic()).getModulus())
                .isEqualTo(((RSAPublicKey) original.getPublic()).getModulus());
    }
}
