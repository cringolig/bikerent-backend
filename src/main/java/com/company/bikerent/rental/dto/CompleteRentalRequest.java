package com.company.bikerent.rental.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public record CompleteRentalRequest(
        @NotNull(message = "End station ID is required")
        @JsonProperty("end_station")
        Long endStationId
) {}
