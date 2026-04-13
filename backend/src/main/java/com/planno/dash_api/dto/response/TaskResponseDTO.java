package com.planno.dash_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskResponseDTO(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        LocalDate dueDate,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean allDay,
        Integer positionIndex,
        Long projectId,
        String projectName,
        UserSummaryResponseDTO responsibleUser,
        UserSummaryResponseDTO createdBy,
        List<UserSummaryResponseDTO> participants,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
