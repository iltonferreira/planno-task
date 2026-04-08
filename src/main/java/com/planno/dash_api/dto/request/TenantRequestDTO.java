package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TenantRequestDTO(

        @NotBlank String name,
        @NotBlank String slug,  // valida oque nao pode ser nulo ou vazio
        String cnpj,
        String billingMode
){}



