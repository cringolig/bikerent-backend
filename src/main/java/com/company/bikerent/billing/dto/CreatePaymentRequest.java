package com.company.bikerent.billing.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreatePaymentRequest(
    @NotNull(message = "Amount is required")
        @Min(value = 1, message = "Amount must be at least 1")
        @Max(value = 999999, message = "Amount cannot exceed 999999")
        @JsonProperty("amount")
        Long amount) {}
