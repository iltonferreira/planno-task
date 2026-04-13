package com.planno.dash_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProjectResponseDTO(
        Long id,
        String name,
        String description,
        String status,
        Double budget,
        LocalDate startDate,
        LocalDate endDate,
        Long clientId,
        String clientName,
        UserSummaryResponseDTO ownerUser,
        UserSummaryResponseDTO createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
