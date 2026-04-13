package com.planno.dash_api.dto.request;

import com.planno.dash_api.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;

public record ProjectRequestDTO(
        @NotBlank String name,
        String description,
        ProjectStatus status,
        @PositiveOrZero Double budget,
        LocalDate startDate,
        LocalDate endDate,
        Long clientId,
        Long ownerUserId
) {}
