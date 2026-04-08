package com.planno.dash_api.dto.response;

import java.time.LocalDateTime;

public record GoogleCalendarConnectionStatusResponseDTO(
        boolean enabled,
        boolean configured,
        boolean connected,
        String googleAccountEmail,
        String defaultCalendarId,
        LocalDateTime expiresAt
) {
}
