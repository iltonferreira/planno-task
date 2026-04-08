package com.planno.dash_api.dto.request;

import com.planno.dash_api.enums.PaymentStatus;
import jakarta.validation.constraints.NotNull;

public record PaymentStatusUpdateRequestDTO(
        @NotNull PaymentStatus status
) {
}
