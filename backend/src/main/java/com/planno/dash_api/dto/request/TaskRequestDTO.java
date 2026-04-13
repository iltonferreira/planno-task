package com.planno.dash_api.dto.request;

import com.planno.dash_api.enums.TaskPriority;
import com.planno.dash_api.enums.TaskStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskRequestDTO(
        @NotBlank String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate dueDate,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Boolean allDay,
        @PositiveOrZero Integer positionIndex,
        Long projectId,
        Long responsibleUserId,
        List<Long> participantIds
) {}
