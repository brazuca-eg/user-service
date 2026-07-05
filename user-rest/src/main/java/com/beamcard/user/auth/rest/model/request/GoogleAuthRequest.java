package com.beamcard.user.auth.rest.model.request;

import jakarta.validation.constraints.NotBlank;

/**
 * @param idToken the Google ID token obtained in the browser via Google Identity Services
 * @param locale the current UI language, assigned to a brand-new user (optional; defaults to en)
 */
public record GoogleAuthRequest(@NotBlank String idToken, String locale) {}
