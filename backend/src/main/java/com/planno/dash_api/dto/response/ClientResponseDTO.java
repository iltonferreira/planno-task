package com.planno.dash_api.dto.response;

public record ClientResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        String document,
        String type,
        Double totalValue,
        boolean active
) {}
