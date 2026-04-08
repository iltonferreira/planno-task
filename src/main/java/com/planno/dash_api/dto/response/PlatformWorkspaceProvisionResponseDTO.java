package com.planno.dash_api.dto.response;

public record PlatformWorkspaceProvisionResponseDTO(
        Long tenantId,
        String tenantName,
        String tenantSlug,
        String billingMode,
        Long adminUserId,
        String adminName,
        String adminEmail,
        String platformStatus,
        String checkoutUrl
) {
}
