package com.planno.dash_api.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PlatformWorkspaceProvisionRequestDTO(
        @NotBlank String tenantName,
        @NotBlank String tenantSlug,
        String tenantCnpj,
        @NotBlank String adminName,
        @NotBlank @Email String adminEmail,
        @NotBlank @Size(min = 11, max = 11, message = "O CPF deve ter exatamente 11 caracteres.") String adminCpf,
        @NotBlank @Size(min = 8, message = "A senha precisa ter pelo menos 8 caracteres.") String adminPassword,
        @NotNull String billingMode
) {
}
