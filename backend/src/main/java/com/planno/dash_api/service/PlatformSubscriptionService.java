package com.planno.dash_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.planno.dash_api.dto.response.PlatformSubscriptionResponseDTO;
import com.planno.dash_api.entity.PlatformSubscription;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.entity.User;
import com.planno.dash_api.enums.PlatformSubscriptionStatus;
import com.planno.dash_api.enums.TenantBillingMode;
import com.planno.dash_api.repository.PlatformSubscriptionRepository;
import com.planno.dash_api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PlatformSubscriptionService {

    private final PlatformSubscriptionRepository repository;
    private final TenantRepository tenantRepository;
    private final CurrentUserService currentUserService;
    private final MercadoPagoService mercadoPagoService;

    @Value("${app.platform-billing.plan-code:planno-tasks-pro-monthly}")
    private String planCode;

    @Value("${app.platform-billing.plan-name:Planno Tasks Pro}")
    private String planName;

    @Value("${app.platform-billing.monthly-price:39.90}")
    private BigDecimal monthlyPrice;

    @Value("${app.platform-billing.currency-id:BRL}")
    private String currencyId;

    @Value("${app.platform-billing.manage-url:http://localhost:4200/workspace-plan}")
    private String manageUrl;

    @Value("${app.platform-billing.notification-url:http://localhost:8080/api/platform-billing/webhooks/mercado-pago}")
    private String notificationUrl;

    @Transactional(readOnly = true)
    public PlatformSubscriptionResponseDTO getCurrentTenantPlan() {
        User currentUser = currentUserService.getCurrentUser();
        return repository.findByTenantId(currentUser.getTenant().getId())
                .map(subscription -> toResponse(subscription, currentUser.getTenant()))
                .orElseGet(() -> emptyResponse(currentUser));
    }

    @Transactional
    public PlatformSubscriptionResponseDTO createOrRefreshCheckout() {
        User currentUser = currentUserService.getCurrentUser();
        Tenant tenant = tenantRepository.findById(currentUser.getTenant().getId())
                .orElseThrow(() -> new IllegalStateException("Tenant nao encontrado para billing."));

        return createOrRefreshCheckoutForTenant(tenant, currentUser.getEmail());
    }

    @Transactional
    public PlatformSubscriptionResponseDTO createOrRefreshCheckoutForTenant(Long tenantId, String payerEmail) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalStateException("Tenant nao encontrado para billing."));

        return createOrRefreshCheckoutForTenant(tenant, payerEmail);
    }

    private PlatformSubscriptionResponseDTO createOrRefreshCheckoutForTenant(Tenant tenant, String payerEmail) {
        if (tenant.getBillingMode() == TenantBillingMode.COMPLIMENTARY) {
            return emptyResponse(new User(null, null, payerEmail, null, null, tenant));
        }

        PlatformSubscription subscription = repository.findByTenantId(tenant.getId())
                .orElseGet(() -> createDraftSubscription(tenant, payerEmail));

        subscription.setPlanCode(planCode);
        subscription.setPlanName(planName);
        subscription.setAmount(monthlyPrice);
        subscription.setCurrencyId(currencyId);
        if (!StringUtils.hasText(subscription.getPayerEmail())) {
            subscription.setPayerEmail(payerEmail);
        }

        MercadoPagoService.SubscriptionCheckoutResult result = mercadoPagoService.createHostedSubscriptionCheckout(
                planName + " - " + tenant.getName(),
                subscription.getExternalReference(),
                subscription.getPayerEmail(),
                subscription.getAmount(),
                manageUrl,
                notificationUrl
        );

        subscription.setExternalSubscriptionId(result.subscriptionId());
        subscription.setCheckoutUrl(result.initPoint());
        subscription.setStatus(mapSubscriptionStatus(result.status()));

        return toResponse(repository.save(subscription), tenant);
    }

    @Transactional
    public void handleMercadoPagoWebhook(JsonNode payload, String requestedDataId) {
        if (!mercadoPagoService.isEnabled()) {
            return;
        }

        String topic = firstText(
                payload == null ? null : payload.path("type").asText(null),
                payload == null ? null : payload.path("topic").asText(null),
                payload == null ? null : payload.path("action").asText(null)
        );
        String dataId = firstText(
                requestedDataId,
                payload == null ? null : payload.path("data").path("id").asText(null),
                payload == null ? null : payload.path("id").asText(null)
        );

        if (!StringUtils.hasText(topic) || !StringUtils.hasText(dataId)) {
            return;
        }

        String normalizedTopic = topic.toLowerCase();
        if (normalizedTopic.contains("subscription_authorized_payment") || normalizedTopic.contains("payment")) {
            syncBillingPaymentFromGateway(dataId);
            return;
        }

        if (normalizedTopic.contains("preapproval") || normalizedTopic.contains("subscription")) {
            syncSubscriptionFromGateway(dataId);
        }
    }

    @Transactional
    public void syncSubscriptionFromGateway(String externalSubscriptionId) {
        MercadoPagoService.RemoteSubscription remoteSubscription = mercadoPagoService.fetchSubscription(externalSubscriptionId);
        PlatformReference reference = parsePlatformReference(remoteSubscription.externalReference());
        if (reference == null) {
            return;
        }

        PlatformSubscription subscription = repository.findFirstByExternalSubscriptionId(externalSubscriptionId)
                .or(() -> repository.findByTenantId(reference.tenantId()))
                .orElseGet(() -> {
                    Tenant tenant = tenantRepository.findById(reference.tenantId())
                            .orElseThrow(() -> new IllegalStateException("Tenant nao encontrado para sincronizacao."));

                    PlatformSubscription draft = createDraftSubscription(tenant, null);
                    draft.setExternalReference(remoteSubscription.externalReference());
                    return draft;
                });

        subscription.setExternalReference(remoteSubscription.externalReference());
        subscription.setExternalSubscriptionId(remoteSubscription.id());
        if (StringUtils.hasText(remoteSubscription.initPoint())) {
            subscription.setCheckoutUrl(remoteSubscription.initPoint());
        }
        subscription.setNextBillingDate(remoteSubscription.nextPaymentDate());
        subscription.setStatus(mapSubscriptionStatus(remoteSubscription.status()));

        repository.save(subscription);
    }

    @Transactional
    public void syncBillingPaymentFromGateway(String externalPaymentId) {
        MercadoPagoService.RemotePayment remotePayment = mercadoPagoService.fetchPayment(externalPaymentId);
        PlatformReference reference = parsePlatformReference(remotePayment.externalReference());
        if (reference == null) {
            return;
        }

        PlatformSubscription subscription = repository.findByTenantId(reference.tenantId())
                .orElseThrow(() -> new IllegalStateException("Assinatura da plataforma nao encontrada para o tenant."));

        if (remotePayment.approvedAt() != null) {
            subscription.setLastPaymentAt(remotePayment.approvedAt());
        }
        if (remotePayment.amount() != null && remotePayment.amount().compareTo(BigDecimal.ZERO) > 0) {
            subscription.setAmount(remotePayment.amount());
        }
        if ("approved".equalsIgnoreCase(remotePayment.status())) {
            subscription.setStatus(PlatformSubscriptionStatus.ACTIVE);
        }

        repository.save(subscription);
    }

    private PlatformSubscription createDraftSubscription(Tenant tenant, String payerEmail) {
        PlatformSubscription subscription = new PlatformSubscription();
        subscription.setTenant(tenant);
        subscription.setPlanCode(planCode);
        subscription.setPlanName(planName);
        subscription.setAmount(monthlyPrice);
        subscription.setCurrencyId(currencyId);
        subscription.setPayerEmail(payerEmail);
        subscription.setStatus(PlatformSubscriptionStatus.PENDING);
        subscription.setExternalReference(buildExternalReference(tenant.getId()));
        return repository.save(subscription);
    }

    private PlatformSubscriptionResponseDTO emptyResponse(User currentUser) {
        Tenant tenant = currentUser.getTenant();
        if (tenant.getBillingMode() == TenantBillingMode.COMPLIMENTARY) {
            return new PlatformSubscriptionResponseDTO(
                    null,
                    "planno-complimentary",
                    "Acesso cortesia",
                    0D,
                    currencyId,
                    "ACTIVE",
                    currentUser.getEmail(),
                    buildExternalReference(tenant.getId()),
                    null,
                    null,
                    null,
                    null,
                    false,
                    currentUserService.isBillingAdminTenant(),
                    tenant.getBillingMode().name()
            );
        }

        return new PlatformSubscriptionResponseDTO(
                null,
                planCode,
                planName,
                monthlyPrice.doubleValue(),
                currencyId,
                "NOT_STARTED",
                currentUser.getEmail(),
                buildExternalReference(currentUser.getTenant().getId()),
                null,
                null,
                null,
                null,
                true,
                currentUserService.isBillingAdminTenant(),
                tenant.getBillingMode().name()
        );
    }

    private PlatformSubscriptionResponseDTO toResponse(PlatformSubscription subscription, Tenant tenant) {
        boolean requiresAction = tenant.getBillingMode() != TenantBillingMode.COMPLIMENTARY
                && (subscription.getStatus() == PlatformSubscriptionStatus.PENDING
                || subscription.getStatus() == PlatformSubscriptionStatus.PAST_DUE
                || !StringUtils.hasText(subscription.getExternalSubscriptionId()));

        return new PlatformSubscriptionResponseDTO(
                subscription.getId(),
                subscription.getPlanCode(),
                subscription.getPlanName(),
                subscription.getAmount().doubleValue(),
                subscription.getCurrencyId(),
                subscription.getStatus().name(),
                subscription.getPayerEmail(),
                subscription.getExternalReference(),
                subscription.getExternalSubscriptionId(),
                subscription.getCheckoutUrl(),
                subscription.getNextBillingDate(),
                subscription.getLastPaymentAt(),
                requiresAction,
                currentUserService.isBillingAdminTenant(),
                tenant.getBillingMode().name()
        );
    }

    private PlatformSubscriptionStatus mapSubscriptionStatus(String gatewayStatus) {
        if (!StringUtils.hasText(gatewayStatus)) {
            return PlatformSubscriptionStatus.PENDING;
        }

        return switch (gatewayStatus.toLowerCase()) {
            case "authorized", "active" -> PlatformSubscriptionStatus.ACTIVE;
            case "paused", "payment_required", "pending" -> PlatformSubscriptionStatus.PAST_DUE;
            case "cancelled", "cancelled_by_admin", "stopped" -> PlatformSubscriptionStatus.CANCELLED;
            default -> PlatformSubscriptionStatus.PENDING;
        };
    }

    private String buildExternalReference(Long tenantId) {
        return "platform:tenant:" + tenantId;
    }

    private PlatformReference parsePlatformReference(String externalReference) {
        if (!StringUtils.hasText(externalReference)) {
            return null;
        }

        String[] tokens = externalReference.split(":");
        if (tokens.length != 3 || !"platform".equals(tokens[0]) || !"tenant".equals(tokens[1])) {
            return null;
        }

        try {
            return new PlatformReference(Long.parseLong(tokens[2]));
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

    private record PlatformReference(Long tenantId) {
    }
}
