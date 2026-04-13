package com.planno.dash_api.dto.response;

import java.time.LocalDateTime;

public record KnowledgeBasePageResponseDTO(
        Long id,
        String title,
        String slug,
        String summary,
        String content,
        boolean pinned,
        int wordCount,
        UserSummaryResponseDTO createdBy,
        UserSummaryResponseDTO updatedBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
