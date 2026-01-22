package com.company.bikerent.station.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.company.bikerent.geo.dto.CoordinatesDto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateStationRequest(
    @NotBlank(message = "Station name is required")
        @Size(max = 100, message = "Station name cannot exceed 100 characters")
        @JsonProperty("name")
        String name,
    @NotNull(message = "Coordinates are required") @Valid @JsonProperty("coordinates")
        CoordinatesDto coordinates) {}
