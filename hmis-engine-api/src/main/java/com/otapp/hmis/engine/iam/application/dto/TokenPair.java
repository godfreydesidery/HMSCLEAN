package com.otapp.hmis.engine.iam.application.dto;

public record TokenPair(String accessToken, String refreshToken, String tokenType) {

    public static TokenPair bearer(String accessToken, String refreshToken) {
        return new TokenPair(accessToken, refreshToken, "Bearer");
    }
}
