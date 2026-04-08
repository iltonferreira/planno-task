package com.planno.dash_api.dto.response;

public record SubscriptionCheckoutResponseDTO(
        Long subscriptionId,
        String checkoutUrl,
        String externalReference,
        String externalSubscriptionId,
        String status
) {
}
