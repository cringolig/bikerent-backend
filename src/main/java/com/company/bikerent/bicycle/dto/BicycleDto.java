package com.company.bikerent.bicycle.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BicycleDto(
    @JsonProperty("id") Long id,
    @JsonProperty("model") String model,
    @JsonProperty("type") String type,
    @JsonProperty("status") String status,
    @JsonProperty("station") Long stationId,
    @JsonProperty("lastServiceDate") Date lastServiceDate,
    @JsonProperty("mileage") Long mileage) {}
