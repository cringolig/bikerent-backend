package com.company.bikerent.billing.dto;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PaymentDto(
    @JsonProperty("id") Long id,
    @JsonProperty("user") Long userId,
    @JsonProperty("amount") Long amount,
    @JsonProperty("paymentDate") Date paymentDate) {}
