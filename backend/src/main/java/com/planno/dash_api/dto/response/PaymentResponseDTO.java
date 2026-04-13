package com.planno.dash_api.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponseDTO(
        Long id,
        String title,
        String description,
        Double amount,
        String type,
        String direction,
        String status,
        String provider,
        LocalDate dueDate,
        LocalDateTime paidAt,
        Long clientId,
        String clientName,
        Long projectId,
        String projectName,
        Long subscriptionId,
        String subscriptionName,
        UserSummaryResponseDTO createdBy,
        String externalReference,
        String externalPaymentId,
        String externalPreferenceId,
        String externalSubscriptionId,
        String checkoutUrl,
        String statusDetail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
