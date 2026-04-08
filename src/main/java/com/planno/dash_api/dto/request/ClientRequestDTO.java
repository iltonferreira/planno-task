package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequestDTO(
        @NotBlank(message = "O nome Ã© obrigatÃ³rio") String name,
        @Email(message = "E-mail invÃ¡lido") String email,
        String phone,
        String document // Opcional como vocÃª pediu
) {}
