package com.beamcard.user.auth.rest.model.response;

public record SignupResponse(boolean verificationRequired, String email, AuthResponse auth) {

    public static SignupResponse pending(String email) {
        return new SignupResponse(true, email, null);
    }

    public static SignupResponse authenticated(AuthResponse auth) {
        return new SignupResponse(false, auth.user().email(), auth);
    }
}
