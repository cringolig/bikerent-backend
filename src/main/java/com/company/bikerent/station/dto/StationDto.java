package com.company.bikerent.station.dto;

import com.company.bikerent.geo.dto.CoordinatesDto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record StationDto(
    @JsonProperty("id") Long id,
    @JsonProperty("name") String name,
    @JsonProperty("coordinates") CoordinatesDto coordinates,
    @JsonProperty("availableBicycles") Long availableBicycles) {}
