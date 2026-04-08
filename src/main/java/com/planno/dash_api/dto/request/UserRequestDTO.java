package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
        @NotBlank String name,
        @Email String email,
        @NotBlank String password,
        @Size(min = 11, max = 11, message = "O CPF deve ter exatamente 11 caracteres") String cpf,
        @NotNull Long tenantId // o ID da empresa que este usuÃ¡rio pertence
) {}
