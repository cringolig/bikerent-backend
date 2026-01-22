package com.company.bikerent.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRegisterRequestRequest(
        @NotBlank(message = "Username is required")
        @JsonProperty(value = "username", required = true)
        String username,
        
        @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        @JsonProperty(value = "description", required = true)
        String description
) {}
