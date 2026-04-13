package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.request.PaymentRequestDTO;
import com.planno.dash_api.dto.response.PaymentResponseDTO;
import com.planno.dash_api.entity.Payment;
import com.planno.dash_api.enums.PaymentDirection;
import com.planno.dash_api.enums.PaymentProvider;
import com.planno.dash_api.enums.PaymentStatus;
import com.planno.dash_api.enums.PaymentType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private final UserMapper userMapper;

    public Payment toEntity(PaymentRequestDTO dto) {
        Payment payment = new Payment();
        applyUpdates(payment, dto);
        return payment;
    }

    public void applyUpdates(Payment payment, PaymentRequestDTO dto) {
        payment.setTitle(dto.title());
        payment.setDescription(dto.description());
        payment.setAmount(dto.amount());
        payment.setType(dto.type() == null ? PaymentType.ONE_TIME : dto.type());
        payment.setDirection(dto.direction() == null ? PaymentDirection.INCOME : dto.direction());
        payment.setProvider(dto.provider() == null ? PaymentProvider.MANUAL : dto.provider());
        payment.setDueDate(dto.dueDate());
        if (payment.getStatus() == null) {
            payment.setStatus(PaymentStatus.PENDING);
        }
    }

    public PaymentResponseDTO toResponse(Payment payment) {
        return new PaymentResponseDTO(
                payment.getId(),
                payment.getTitle(),
                payment.getDescription(),
                payment.getAmount().doubleValue(),
                payment.getType().name(),
                payment.getDirection().name(),
                payment.getStatus().name(),
                payment.getProvider().name(),
                payment.getDueDate(),
                payment.getPaidAt(),
                payment.getClient() == null ? null : payment.getClient().getId(),
                payment.getClient() == null ? null : payment.getClient().getName(),
                payment.getProject() == null ? null : payment.getProject().getId(),
                payment.getProject() == null ? null : payment.getProject().getName(),
                payment.getSubscription() == null ? null : payment.getSubscription().getId(),
                payment.getSubscription() == null ? null : payment.getSubscription().getDescription(),
                userMapper.toSummary(payment.getCreatedBy()),
                payment.getExternalReference(),
                payment.getExternalPaymentId(),
                payment.getExternalPreferenceId(),
                payment.getExternalSubscriptionId(),
                payment.getCheckoutUrl(),
                payment.getStatusDetail(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
