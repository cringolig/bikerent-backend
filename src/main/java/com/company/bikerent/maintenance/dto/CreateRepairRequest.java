package com.company.bikerent.maintenance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateRepairRequest(
    @NotNull(message = "Bicycle ID is required") @JsonProperty("bicycleId") Long bicycleId,
    @NotNull(message = "Technician ID is required") @JsonProperty("technicianId") Long technicianId,
    @NotBlank(message = "Description is required")
        @Size(max = 500, message = "Description cannot exceed 500 characters")
        @JsonProperty("description")
        String description) {}
