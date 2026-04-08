package com.planno.dash_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.planno.dash_api.dto.mapper.PaymentMapper;
import com.planno.dash_api.dto.request.PaymentRequestDTO;
import com.planno.dash_api.dto.response.PaymentResponseDTO;
import com.planno.dash_api.entity.Client;
import com.planno.dash_api.entity.Payment;
import com.planno.dash_api.entity.Project;
import com.planno.dash_api.entity.Subscription;
import com.planno.dash_api.enums.PaymentDirection;
import com.planno.dash_api.enums.PaymentProvider;
import com.planno.dash_api.enums.PaymentStatus;
import com.planno.dash_api.enums.PaymentType;
import com.planno.dash_api.enums.SubscriptionStatus;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.ClientRepository;
import com.planno.dash_api.repository.PaymentRepository;
import com.planno.dash_api.repository.ProjectRepository;
import com.planno.dash_api.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CurrentUserService currentUserService;
    private final PaymentMapper mapper;
    private final NotificationService notificationService;
    private final MercadoPagoService mercadoPagoService;

    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> findAll() {
        Long tenantId = currentUserService.getCurrentTenantId();
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponseDTO findById(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        return mapper.toResponse(getPayment(id, tenantId));
    }

    @Transactional
    public PaymentResponseDTO save(PaymentRequestDTO dto) {
        var currentUser = currentUserService.getCurrentUser();
        Long tenantId = currentUser.getTenant().getId();

        if (dto.provider() == PaymentProvider.MERCADO_PAGO || Boolean.TRUE.equals(dto.generateCheckout())) {
            throw new BusinessException("O checkout Mercado Pago do tenant foi desativado. Use a area Meu plano para o billing da plataforma.");
        }

        Payment payment = mapper.toEntity(dto);
        payment.setTenant(currentUser.getTenant());
        payment.setCreatedBy(currentUser);
        payment.setClient(resolveClient(dto.clientId(), tenantId));
        payment.setProject(resolveProject(dto.projectId(), tenantId));
        payment.setSubscription(resolveSubscription(dto.subscriptionId(), tenantId));

        if (payment.getSubscription() != null) {
            payment.setType(PaymentType.SUBSCRIPTION);
        }

        Payment saved = repository.save(payment);
        saved.setExternalReference(buildExternalReference(saved));

        return mapper.toResponse(repository.save(saved));
    }

    @Transactional
    public PaymentResponseDTO updateStatus(Long id, PaymentStatus status) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Payment payment = getPayment(id, tenantId);
        PaymentStatus previousStatus = payment.getStatus();

        applyStatus(payment, status, payment.getStatusDetail(), payment.getPaidAt());
        Payment saved = repository.save(payment);
        notifyPaymentReceived(previousStatus, saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void handleMercadoPagoWebhook(JsonNode payload, String requestedDataId) {
        String type = firstText(
                payload.path("type").asText(null),
                payload.path("topic").asText(null),
                payload.path("action").asText(null)
        );
        String dataId = firstText(
                requestedDataId,
                payload.path("data").path("id").asText(null),
                payload.path("id").asText(null)
        );

        if (!StringUtils.hasText(type) || !StringUtils.hasText(dataId) || !mercadoPagoService.isEnabled()) {
            return;
        }

        String normalizedType = type.toLowerCase();
        if (normalizedType.contains("payment")) {
            syncPaymentFromGateway(dataId);
            return;
        }

        if (normalizedType.contains("preapproval") || normalizedType.contains("subscription")) {
            syncSubscriptionFromGateway(dataId);
        }
    }

    @Transactional
    public void syncPaymentFromGateway(String externalPaymentId) {
        MercadoPagoService.RemotePayment remotePayment = mercadoPagoService.fetchPayment(externalPaymentId);
        ExternalReference reference = parseExternalReference(remotePayment.externalReference());

        Payment payment = repository.findFirstByExternalPaymentId(externalPaymentId)
                .orElseGet(() -> findOrCreateByReference(reference, remotePayment));

        PaymentStatus previousStatus = payment.getStatus();
        payment.setExternalPaymentId(remotePayment.id());
        payment.setExternalReference(remotePayment.externalReference());
        payment.setStatusDetail(remotePayment.statusDetail());
        if (StringUtils.hasText(remotePayment.description()) && !StringUtils.hasText(payment.getDescription())) {
            payment.setDescription(remotePayment.description());
        }
        if (remotePayment.amount() != null && remotePayment.amount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            payment.setAmount(remotePayment.amount());
        }
        applyStatus(payment, mapPaymentStatus(remotePayment.status()), remotePayment.statusDetail(), remotePayment.approvedAt());

        Payment saved = repository.save(payment);
        notifyPaymentReceived(previousStatus, saved);
    }

    @Transactional
    public void syncSubscriptionFromGateway(String externalSubscriptionId) {
        MercadoPagoService.RemoteSubscription remoteSubscription = mercadoPagoService.fetchSubscription(externalSubscriptionId);
        ExternalReference reference = parseExternalReference(remoteSubscription.externalReference());

        Subscription subscription = resolveSubscriptionFromReference(reference, externalSubscriptionId);
        subscription.setExternalReference(remoteSubscription.externalReference());
        subscription.setExternalSubscriptionId(remoteSubscription.id());
        if (StringUtils.hasText(remoteSubscription.initPoint())) {
            subscription.setCheckoutUrl(remoteSubscription.initPoint());
        }
        subscription.setStatus(mapSubscriptionStatus(remoteSubscription.status()));

        subscriptionRepository.save(subscription);
        notificationService.notifySubscriptionUpdated(subscription);
    }

    private Payment findOrCreateByReference(ExternalReference reference, MercadoPagoService.RemotePayment remotePayment) {
        if (reference == null) {
            throw new ResourceNotFoundException("Pagamento remoto nao corresponde a nenhum registro local.");
        }

        if ("payment".equals(reference.type())) {
            return repository.findByIdAndTenantId(reference.entityId(), reference.tenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Pagamento nao encontrado para sincronizacao."));
        }

        if ("subscription".equals(reference.type())) {
            Subscription subscription = subscriptionRepository.findByIdAndTenantId(reference.entityId(), reference.tenantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assinatura nao encontrada para sincronizacao."));

            Payment payment = new Payment();
            payment.setTitle(subscription.getDescription());
            payment.setDescription(remotePayment.description());
            payment.setAmount(remotePayment.amount());
            payment.setType(PaymentType.SUBSCRIPTION);
            payment.setDirection(PaymentDirection.INCOME);
            payment.setStatus(PaymentStatus.PENDING);
            payment.setProvider(PaymentProvider.MERCADO_PAGO);
            payment.setClient(subscription.getClient());
            payment.setSubscription(subscription);
            payment.setTenant(subscription.getTenant());
            payment.setExternalReference(remotePayment.externalReference());
            payment.setExternalSubscriptionId(subscription.getExternalSubscriptionId());
            return repository.save(payment);
        }

        throw new ResourceNotFoundException("Referencia externa nao suportada.");
    }

    private Subscription resolveSubscriptionFromReference(ExternalReference reference, String externalSubscriptionId) {
        if (StringUtils.hasText(externalSubscriptionId)) {
            var existing = subscriptionRepository.findFirstByExternalSubscriptionId(externalSubscriptionId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        if (reference == null || !"subscription".equals(reference.type())) {
            throw new ResourceNotFoundException("Assinatura remota nao corresponde a nenhum registro local.");
        }

        return subscriptionRepository.findByIdAndTenantId(reference.entityId(), reference.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura nao encontrada para sincronizacao."));
    }

    private Payment getPayment(Long id, Long tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento nao encontrado."));
    }

    private Client resolveClient(Long clientId, Long tenantId) {
        if (clientId == null) {
            return null;
        }

        return clientRepository.findByIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado para este pagamento."));
    }

    private Project resolveProject(Long projectId, Long tenantId) {
        if (projectId == null) {
            return null;
        }

        return projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto nao encontrado para este pagamento."));
    }

    private Subscription resolveSubscription(Long subscriptionId, Long tenantId) {
        if (subscriptionId == null) {
            return null;
        }

        return subscriptionRepository.findByIdAndTenantId(subscriptionId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura nao encontrada para este pagamento."));
    }

    private String buildExternalReference(Payment payment) {
        return "tenant:" + payment.getTenant().getId() + ":payment:" + payment.getId();
    }

    private PaymentStatus mapPaymentStatus(String gatewayStatus) {
        if (!StringUtils.hasText(gatewayStatus)) {
            return PaymentStatus.PENDING;
        }

        return switch (gatewayStatus.toLowerCase()) {
            case "approved" -> PaymentStatus.APPROVED;
            case "in_process" -> PaymentStatus.IN_PROCESS;
            case "rejected" -> PaymentStatus.REJECTED;
            case "cancelled" -> PaymentStatus.CANCELLED;
            case "refunded", "charged_back" -> PaymentStatus.REFUNDED;
            default -> PaymentStatus.PENDING;
        };
    }

    private SubscriptionStatus mapSubscriptionStatus(String gatewayStatus) {
        if (!StringUtils.hasText(gatewayStatus)) {
            return SubscriptionStatus.PAUSED;
        }

        return switch (gatewayStatus.toLowerCase()) {
            case "authorized", "active" -> SubscriptionStatus.ACTIVE;
            case "paused" -> SubscriptionStatus.PAUSED;
            case "cancelled", "cancelled_by_admin", "stopped" -> SubscriptionStatus.CANCELLED;
            case "pending", "payment_required" -> SubscriptionStatus.OVERDUE;
            default -> SubscriptionStatus.PAUSED;
        };
    }

    private void applyStatus(Payment payment, PaymentStatus status, String statusDetail, LocalDateTime paidAt) {
        payment.setStatus(status);
        payment.setStatusDetail(statusDetail);
        if (status == PaymentStatus.APPROVED) {
            payment.setPaidAt(paidAt != null ? paidAt : LocalDateTime.now());
        }
    }

    private void notifyPaymentReceived(PaymentStatus previousStatus, Payment payment) {
        if (previousStatus == PaymentStatus.APPROVED || payment.getStatus() != PaymentStatus.APPROVED) {
            return;
        }

        if (payment.getDirection() != PaymentDirection.INCOME || payment.getClient() == null) {
            return;
        }

        notificationService.notifyPaymentReceived(
                payment.getClient().getEmail(),
                payment.getClient().getName(),
                payment.getTitle(),
                String.valueOf(payment.getAmount()),
                payment.getStatus().name()
        );
    }

    private ExternalReference parseExternalReference(String externalReference) {
        if (!StringUtils.hasText(externalReference)) {
            return null;
        }

        String[] tokens = externalReference.split(":");
        if (tokens.length != 4 || !"tenant".equals(tokens[0])) {
            return null;
        }

        try {
            return new ExternalReference(Long.parseLong(tokens[1]), tokens[2], Long.parseLong(tokens[3]));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    private record ExternalReference(
            Long tenantId,
            String type,
            Long entityId
    ) {
    }
}
