package com.planno.dash_api.dto.response;

import java.util.List;

public record PlatformBillingAdminOverviewResponseDTO(
        String adminTenantName,
        int totalCustomers,
        int activeCustomers,
        Double projectedMonthlyRevenue,
        Double pendingMonthlyRevenue,
        int mercadoPagoSubscriptionCount,
        int mercadoPagoActiveSubscriptionCount,
        Double mercadoPagoProjectedRevenue,
        List<PlatformBillingTenantItemDTO> customers,
        List<MercadoPagoSubscriptionItemDTO> mercadoPagoSubscriptions
) {
}
