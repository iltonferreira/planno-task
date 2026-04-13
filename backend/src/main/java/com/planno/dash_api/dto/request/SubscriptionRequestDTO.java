package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SubscriptionRequestDTO(
        @NotBlank String description,
        @Positive BigDecimal price,
        @NotNull Long clientId,
        LocalDate nextBillingDate,
        String externalReference
) {}

