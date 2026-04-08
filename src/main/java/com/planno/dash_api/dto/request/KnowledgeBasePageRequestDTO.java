package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record KnowledgeBasePageRequestDTO(
        @NotBlank String title,
        String summary,
        @NotBlank String content,
        Boolean pinned
) {
}
