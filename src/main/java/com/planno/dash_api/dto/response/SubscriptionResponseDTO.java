package com.planno.dash_api.dto.response;

import java.time.LocalDate;

public record SubscriptionResponseDTO(
        Long id,
        String description,
        Double price,
        String status,
        LocalDate nextBillingDate,
        String externalReference,
        String externalSubscriptionId,
        String checkoutUrl,
        Long clientId,
        String clientName
) {}
