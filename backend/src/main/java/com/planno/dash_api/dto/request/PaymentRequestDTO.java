package com.planno.dash_api.dto.request;

import com.planno.dash_api.enums.PaymentDirection;
import com.planno.dash_api.enums.PaymentProvider;
import com.planno.dash_api.enums.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentRequestDTO(
        @NotBlank String title,
        String description,
        @NotNull @Positive BigDecimal amount,
        PaymentType type,
        PaymentDirection direction,
        PaymentProvider provider,
        LocalDate dueDate,
        Long clientId,
        Long projectId,
        Long subscriptionId,
        String payerEmail,
        Boolean generateCheckout
) {
}
