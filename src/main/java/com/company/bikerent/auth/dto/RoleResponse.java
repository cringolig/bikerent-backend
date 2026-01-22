package com.company.bikerent.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record RoleResponse(
    @JsonProperty("username") String username, @JsonProperty("role") String role) {}
