package com.planno.dash_api.dto.response;

public record ClientResponseDTO(
        Long id,
        String name,
        String email,
        String phone,
        String document,
        String type,       // "ASSINATURA", "AVULSO" ou "HIBRIDO"
        Double totalValue, // Soma total que esse cliente jÃ¡ rendeu
        boolean active
) {}
