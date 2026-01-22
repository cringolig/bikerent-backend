package com.company.bikerent.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/** Response containing access and refresh tokens. */
@Schema(description = "Authentication tokens response")
public record TokenResponse(
    @Schema(description = "JWT access token", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        @JsonProperty("access_token")
        String accessToken,
    @Schema(
            description = "Refresh token for obtaining new access tokens",
            example = "550e8400-e29b-41d4-a716-446655440000")
        @JsonProperty("refresh_token")
        String refreshToken,
    @Schema(description = "Token type", example = "Bearer") @JsonProperty("token_type")
        String tokenType,
    @Schema(description = "Access token expiration time in seconds", example = "900")
        @JsonProperty("expires_in")
        Long expiresIn,
    @Schema(description = "User ID", example = "1") @JsonProperty("user_id") Long userId,
    @Schema(description = "Username", example = "john_doe") @JsonProperty("username")
        String username) {
  public static TokenResponse of(
      String accessToken, String refreshToken, Long expiresInMs, Long userId, String username) {
    return new TokenResponse(
        accessToken, refreshToken, "Bearer", expiresInMs / 1000, userId, username);
  }
}
