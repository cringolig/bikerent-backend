package com.company.bikerent.auth.dto;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/** Request to refresh access token using a refresh token. */
@Schema(description = "Refresh token request")
public record RefreshTokenRequest(
    @Schema(
            description = "Refresh token",
            example = "550e8400-e29b-41d4-a716-446655440000",
            required = true)
        @NotBlank(message = "Refresh token is required")
        @JsonProperty("refresh_token")
        String refreshToken) {}
