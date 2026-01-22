package com.company.bikerent.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/** Request for user registration. */
@Schema(description = "User registration request")
public record RegisterRequest(
    @Schema(
            description = "Username (3-50 characters)",
            example = "john_doe",
            minLength = 3,
            maxLength = 50)
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
        @Pattern(
            regexp = "^[a-zA-Z0-9_]+$",
            message = "Username can only contain letters, numbers, and underscores")
        @JsonProperty(value = "username", required = true)
        String username,
    @Schema(
            description = "Password (min 8 characters, must include letter and number)",
            example = "SecurePass123",
            minLength = 8)
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
            message = "Password must contain at least one letter and one number")
        @JsonProperty(value = "password", required = true)
        String password) {}
