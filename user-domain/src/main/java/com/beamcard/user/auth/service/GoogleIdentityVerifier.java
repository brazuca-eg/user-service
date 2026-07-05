package com.beamcard.user.auth.service;

public interface GoogleIdentityVerifier {

    GoogleIdentity verify(String idToken);

    record GoogleIdentity(String sub, String email, boolean emailVerified) {}
}
