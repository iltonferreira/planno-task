package com.planno.dash_api.dto.response;

import java.time.LocalDate;

public record MercadoPagoSubscriptionItemDTO(
        String id,
        String reason,
        String status,
        String payerEmail,
        String externalReference,
        String initPoint,
        Double amount,
        String currencyId,
        LocalDate nextPaymentDate,
        Long linkedTenantId,
        String linkedTenantName
) {
}
