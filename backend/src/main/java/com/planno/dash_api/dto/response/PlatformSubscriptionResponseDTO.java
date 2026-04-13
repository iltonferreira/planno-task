package com.planno.dash_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlatformSubscriptionResponseDTO(
        Long id,
        String planCode,
        String planName,
        Double amount,
        String currencyId,
        String status,
        String payerEmail,
        String externalReference,
        String externalSubscriptionId,
        String checkoutUrl,
        LocalDate nextBillingDate,
        LocalDateTime lastPaymentAt,
        boolean requiresAction,
        boolean billingAdminTenant,
        String billingMode
) {
}
