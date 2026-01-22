package com.company.bikerent.bicycle.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public record BicycleDto(
        @JsonProperty("id")
        Long id,

        @JsonProperty("model")
        String model,

        @JsonProperty("type")
        String type,

        @JsonProperty("status")
        String status,

        @JsonProperty("station")
        Long stationId,

        @JsonProperty("lastServiceDate")
        Date lastServiceDate,

        @JsonProperty("mileage")
        Long mileage
) {}
