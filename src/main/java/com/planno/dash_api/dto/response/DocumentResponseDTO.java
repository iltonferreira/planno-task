package com.planno.dash_api.dto.response;

import java.time.LocalDateTime;

public record DocumentResponseDTO(
        Long id,
        String name,
        String mimeType,
        Long fileSize,
        String storageFileId,
        String storageFolderId,
        String storageFolderPath,
        String webViewUrl,
        String relationType,
        Long relationId,
        UserSummaryResponseDTO uploadedBy,
        LocalDateTime createdAt
) {
}
