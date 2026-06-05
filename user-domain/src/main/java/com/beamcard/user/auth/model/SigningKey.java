package com.beamcard.user.auth.model;

import java.security.KeyPair;

public record SigningKey(KeyPair keyPair, String keyId) {}
