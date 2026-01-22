package com.company.bikerent.rental.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record CreateRentalRequest(
        @NotNull(message = "User ID is required")
        @JsonProperty("user")
        Long userId,

        @NotNull(message = "Bicycle ID is required")
        @JsonProperty("bicycle")
        Long bicycleId,

        @NotNull(message = "Start station ID is required")
        @JsonProperty("start_station")
        Long startStationId
) {}
