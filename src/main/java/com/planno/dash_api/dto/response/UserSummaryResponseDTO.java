package com.planno.dash_api.dto.response;

public record UserSummaryResponseDTO(
        Long id,
        String name,
        String email
) {}
