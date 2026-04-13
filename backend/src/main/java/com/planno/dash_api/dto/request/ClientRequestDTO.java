package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ClientRequestDTO(
        @NotBlank(message = "O nome e obrigatorio") String name,
        @Email(message = "E-mail invalido") String email,
        String phone,
        String document
) {}
