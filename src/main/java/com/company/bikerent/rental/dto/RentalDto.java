package com.company.bikerent.rental.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record RentalDto(
        @JsonProperty("id")
        Long id,

        @JsonProperty("user")
        Long userId,

        @JsonProperty("bicycle")
        Long bicycleId,

        @JsonProperty("start_station")
        Long startStationId,

        @JsonProperty("end_station")
        Long endStationId,

        @JsonProperty("status")
        String status,

        @JsonProperty("started_at")
        LocalDateTime startedAt,

        @JsonProperty("ended_at")
        LocalDateTime endedAt,

        @JsonProperty("cost")
        Double cost
) {}
