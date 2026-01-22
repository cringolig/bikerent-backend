package com.company.bikerent.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTechnicianRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 100, message = "Name cannot exceed 100 characters")
        @JsonProperty("name")
        String name,
        
        @NotBlank(message = "Phone is required")
        @Size(max = 20, message = "Phone cannot exceed 20 characters")
        @JsonProperty("phone")
        String phone,
        
        @NotBlank(message = "Specialization is required")
        @Size(max = 100, message = "Specialization cannot exceed 100 characters")
        @JsonProperty("specialization")
        String specialization
) {}
