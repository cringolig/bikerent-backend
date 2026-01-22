package com.company.bikerent.bicycle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateBicycleRequest(
    @NotBlank(message = "Model is required")
        @Size(max = 100, message = "Model cannot exceed 100 characters")
        @JsonProperty("model")
        String model,
    @NotBlank(message = "Type is required") @JsonProperty("type") String type,
    @JsonProperty("status") String status,
    @NotNull(message = "Station ID is required") @JsonProperty("stationId") Long stationId) {}
