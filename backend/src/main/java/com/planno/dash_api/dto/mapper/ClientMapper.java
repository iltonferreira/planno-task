package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.request.ClientRequestDTO;
import com.planno.dash_api.dto.response.ClientResponseDTO;
import com.planno.dash_api.entity.Client;
import com.planno.dash_api.entity.Payment;
import com.planno.dash_api.entity.Subscription;
import com.planno.dash_api.enums.PaymentDirection;
import com.planno.dash_api.enums.PaymentStatus;
import com.planno.dash_api.enums.SubscriptionStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class ClientMapper {

    public Client toEntity(ClientRequestDTO dto) {
        Client client = new Client();
        client.setName(dto.name());
        client.setEmail(dto.email());
        client.setPhone(dto.phone());
        client.setDocument(dto.document());
        client.setActive(true);
        return client;
    }

    public ClientResponseDTO toResponse(Client client) {
        BigDecimal totalSubscriptions = client.getSubscriptions() != null ?
                client.getSubscriptions().stream()
                        .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                        .map(Subscription::getPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        BigDecimal totalApprovedPayments = client.getPayments() != null ?
                client.getPayments().stream()
                        .filter(payment -> payment.getDirection() == PaymentDirection.INCOME)
                        .filter(payment -> payment.getStatus() == PaymentStatus.APPROVED)
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add) : BigDecimal.ZERO;

        String type = "SEM SERVICO";
        if (client.getSubscriptions() != null && !client.getSubscriptions().isEmpty()) {
            type = "ASSINATURA";
        }
        if (totalApprovedPayments.compareTo(BigDecimal.ZERO) > 0 && "ASSINATURA".equals(type)) {
            type = "HIBRIDO";
        } else if (totalApprovedPayments.compareTo(BigDecimal.ZERO) > 0) {
            type = "AVULSO";
        }
        return new ClientResponseDTO(
                client.getId(),
                client.getName(),
                client.getEmail(),
                client.getPhone(),
                client.getDocument(),
                type,
                totalSubscriptions.add(totalApprovedPayments).doubleValue(),
                client.isActive()
        );
    }
}

