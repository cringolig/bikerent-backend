package com.company.bikerent.maintenance.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RepairDto(
    @JsonProperty("id") Long id,
    @JsonProperty("bicycle") Long bicycleId,
    @JsonProperty("technician") Long technicianId,
    @JsonProperty("description") String description,
    @JsonProperty("status") String status,
    @JsonProperty("started_at") LocalDateTime startedAt,
    @JsonProperty("ended_at") LocalDateTime endedAt) {}
