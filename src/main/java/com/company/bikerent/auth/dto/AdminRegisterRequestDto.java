package com.company.bikerent.auth.dto;

import java.time.LocalDateTime;

import com.company.bikerent.auth.domain.RequestStatus;
import com.company.bikerent.user.dto.UserDto;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AdminRegisterRequestDto(
    @JsonProperty("request_id") Long requestId,
    @JsonProperty("user") UserDto user,
    @JsonProperty("description") String description,
    @JsonProperty("status") RequestStatus status,
    @JsonProperty("created_date") LocalDateTime createdDate,
    @JsonProperty("updated_date") LocalDateTime updatedDate) {}
