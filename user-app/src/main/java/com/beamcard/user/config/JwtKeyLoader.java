package com.beamcard.user.config;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.converter.RsaKeyConverters;

@Slf4j
@AllArgsConstructor
class JwtKeyLoader {

    private final JwtProperties properties;

    KeyPair load() {
        if (properties.hasInlinePem()) {
            return readInline();
        }

        Path privatePath = Path.of(properties.privateKeyLocation());
        Path publicPath = Path.of(properties.publicKeyLocation());

        if (Files.exists(privatePath) && Files.exists(publicPath)) {
            return read(privatePath, publicPath);
        }
        if (!properties.generateIfMissing()) {
            throw new IllegalStateException(("JWT signing keys not found "));
        }
        return generateAndPersist(privatePath, publicPath);
    }

    // Prod path: PEM contents come straight from env vars
    private KeyPair readInline() {
        RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(pemStream(properties.privateKeyPem()));
        RSAPublicKey publicKey = RsaKeyConverters.x509().convert(pemStream(properties.publicKeyPem()));
        log.debug("Loaded JWT signing key from inline PEM");
        return new KeyPair(publicKey, privateKey);
    }

    private static InputStream pemStream(String pem) {
        String normalized = pem.replace("\\n", "\n").trim();
        return new ByteArrayInputStream(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private KeyPair read(Path privatePath, Path publicPath) {
        try (InputStream priv = Files.newInputStream(privatePath);
                InputStream pub = Files.newInputStream(publicPath)) {
            RSAPrivateKey privateKey = RsaKeyConverters.pkcs8().convert(priv);
            RSAPublicKey publicKey = RsaKeyConverters.x509().convert(pub);
            log.debug("Loaded JWT signing key from {}", privatePath);
            return new KeyPair(publicKey, privateKey);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read JWT signing keys from %s".formatted(privatePath), e);
        }
    }

    private KeyPair generateAndPersist(Path privatePath, Path publicPath) {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            if (privatePath.getParent() != null) {
                Files.createDirectories(privatePath.getParent());
            }
            writePem(privatePath, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
            writePem(publicPath, "PUBLIC KEY", keyPair.getPublic().getEncoded());
            log.warn("Generated a new dev JWT keypair at {} — do not use in production", privatePath);
            return keyPair;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate JWT signing keys at %s".formatted(privatePath), e);
        }
    }

    private static void writePem(Path path, String type, byte[] der) throws IOException {
        String body = Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(der);
        Files.writeString(path, "-----BEGIN %s-----%n%s%n-----END %s-----%n".formatted(type, body, type));
    }
}
