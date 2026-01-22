package com.company.bikerent.billing.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

public record PaymentDto(
        @JsonProperty("id")
        Long id,
        
        @JsonProperty("user")
        Long userId,

        @JsonProperty("amount")
        Long amount,

        @JsonProperty("paymentDate")
        Date paymentDate
) {}
