package com.planno.dash_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GoogleCalendarEventResponseDTO(
        String calendarId,
        String eventId,
        String summary,
        String description,
        String status,
        String htmlLink,
        boolean allDay,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime startAt,
        LocalDateTime endAt,
        Long linkedTaskId
) {
}
