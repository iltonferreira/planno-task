package com.planno.dash_api.dto.request;

import com.planno.dash_api.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TaskStatusUpdateRequestDTO(
        @NotNull TaskStatus status,
        @PositiveOrZero Integer positionIndex
) {}
