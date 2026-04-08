package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlatformPaymentLinkRequestDTO(
        Long tenantId,
        @NotBlank String title,
        String description,
        @Email String payerEmail,
        @NotNull @Positive BigDecimal amount
) {
}
