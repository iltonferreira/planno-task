package com.planno.dash_api.dto.response;

public record PlatformPaymentLinkResponseDTO(
        String externalReference,
        String preferenceId,
        String checkoutUrl
) {
}
