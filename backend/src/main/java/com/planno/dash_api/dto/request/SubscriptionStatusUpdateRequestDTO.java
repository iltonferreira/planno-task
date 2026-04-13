package com.planno.dash_api.dto.request;

import com.planno.dash_api.enums.SubscriptionStatus;
import jakarta.validation.constraints.NotNull;

public record SubscriptionStatusUpdateRequestDTO(
        @NotNull SubscriptionStatus status
) {}
