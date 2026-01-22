package com.company.bikerent.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Username is required")
        @JsonProperty(value = "username", required = true)
        String username,
        
        @NotBlank(message = "Password is required")
        @JsonProperty(value = "password", required = true)
        String password
) {}
