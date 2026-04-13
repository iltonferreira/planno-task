package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TenantRequestDTO(

        @NotBlank String name,
        @NotBlank String slug,
        String cnpj,
        String billingMode
){}



