package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.request.SubscriptionRequestDTO;
import com.planno.dash_api.dto.response.SubscriptionResponseDTO;
import com.planno.dash_api.entity.Subscription;
import com.planno.dash_api.enums.SubscriptionStatus;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionMapper {

    public Subscription toEntity(SubscriptionRequestDTO dto) {
        Subscription subscription = new Subscription();
        subscription.setDescription(dto.description());
        subscription.setPrice(dto.price());
        subscription.setNextBillingDate(dto.nextBillingDate());
        subscription.setExternalReference(dto.externalReference());

        // Toda assinatura nova comeÃ§a como ATIVA por padrÃ£o
        subscription.setStatus(SubscriptionStatus.ACTIVE);

        return subscription;
    }

    public SubscriptionResponseDTO toResponse(Subscription subscription) {
        return new SubscriptionResponseDTO(
                subscription.getId(),
                subscription.getDescription(),
                subscription.getPrice().doubleValue(),
                subscription.getStatus().name(),
                subscription.getNextBillingDate(),
                subscription.getExternalReference(),
                subscription.getExternalSubscriptionId(),
                subscription.getCheckoutUrl(),
                subscription.getClient().getId(),
                subscription.getClient().getName() // Facilita a vida do Front-end
        );
    }
}

