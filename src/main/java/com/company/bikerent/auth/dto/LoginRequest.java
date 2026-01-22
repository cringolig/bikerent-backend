package com.company.bikerent.auth.dto;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LoginRequest(
    @NotBlank(message = "Username is required") @JsonProperty(value = "username", required = true)
        String username,
    @NotBlank(message = "Password is required") @JsonProperty(value = "password", required = true)
        String password) {}
