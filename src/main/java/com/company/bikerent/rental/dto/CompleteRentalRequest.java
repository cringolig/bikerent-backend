package com.company.bikerent.rental.dto;

import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CompleteRentalRequest(
    @NotNull(message = "End station ID is required") @JsonProperty("end_station")
        Long endStationId) {}
