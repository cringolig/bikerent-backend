package com.company.bikerent.maintenance.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TechnicianDto(
        @JsonProperty("id")
        Long id,
        
        @JsonProperty("name")
        String name,
        
        @JsonProperty("phone")
        String phone,
        
        @JsonProperty("specialization")
        String specialization
) {}
