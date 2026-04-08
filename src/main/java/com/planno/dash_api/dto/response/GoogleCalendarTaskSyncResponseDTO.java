package com.planno.dash_api.dto.response;

import java.time.LocalDateTime;

public record GoogleCalendarTaskSyncResponseDTO(
        Long taskId,
        String calendarId,
        String eventId,
        String htmlLink,
        LocalDateTime syncedAt
) {
}
