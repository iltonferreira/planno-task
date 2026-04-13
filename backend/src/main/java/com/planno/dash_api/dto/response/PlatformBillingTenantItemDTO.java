package com.planno.dash_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PlatformBillingTenantItemDTO(
        Long tenantId,
        String tenantName,
        String tenantSlug,
        boolean tenantActive,
        String billingMode,
        String planName,
        Double amount,
        String currencyId,
        String status,
        String adminName,
        String adminEmail,
        String payerEmail,
        String externalReference,
        String externalSubscriptionId,
        String checkoutUrl,
        LocalDate nextBillingDate,
        LocalDateTime lastPaymentAt
) {
}
