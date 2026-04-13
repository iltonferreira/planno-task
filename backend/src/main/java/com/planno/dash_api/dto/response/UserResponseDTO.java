package com.planno.dash_api.dto.response;

public record UserResponseDTO(
        Long id,
        String name,
        String email,
        String cpf,
        Long tenantId, // Id da empresa para facilitar buscas no front
        String tenantName // Nome da empresa para exibir no perfil/dashboard
) {}

