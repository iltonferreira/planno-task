package com.planno.dash_api.dto.response;

import java.time.LocalDateTime;

public record GoogleDriveConnectionStatusResponseDTO(
        boolean enabled,
        boolean configured,
        boolean connected,
        String googleAccountEmail,
        String rootFolderId,
        LocalDateTime expiresAt
) {
}
