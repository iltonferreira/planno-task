package com.planno.dash_api.service;

import com.planno.dash_api.dto.request.PlatformPaymentLinkRequestDTO;
import com.planno.dash_api.dto.request.PlatformWorkspaceProvisionRequestDTO;
import com.planno.dash_api.dto.response.MercadoPagoSubscriptionItemDTO;
import com.planno.dash_api.dto.response.PlatformBillingAdminOverviewResponseDTO;
import com.planno.dash_api.dto.response.PlatformBillingTenantItemDTO;
import com.planno.dash_api.dto.response.PlatformPaymentLinkResponseDTO;
import com.planno.dash_api.dto.response.PlatformSubscriptionResponseDTO;
import com.planno.dash_api.dto.response.PlatformWorkspaceProvisionResponseDTO;
import com.planno.dash_api.entity.PlatformSubscription;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.entity.User;
import com.planno.dash_api.enums.TenantBillingMode;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.PlatformSubscriptionRepository;
import com.planno.dash_api.repository.TenantRepository;
import com.planno.dash_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlatformBillingAdminService {

    private final CurrentUserService currentUserService;
    private final PlatformSubscriptionRepository repository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformSubscriptionService platformSubscriptionService;
    private final MercadoPagoService mercadoPagoService;

    @Value("${app.platform-billing.plan-name:Planno Pro}")
    private String planName;

    @Value("${app.platform-billing.monthly-price:39.90}")
    private BigDecimal monthlyPrice;

    @Value("${app.platform-billing.currency-id:BRL}")
    private String currencyId;

    @Transactional(readOnly = true)
    public PlatformBillingAdminOverviewResponseDTO getOverview() {
        currentUserService.ensureBillingAdminTenant();
        Long currentTenantId = currentUserService.getCurrentTenantId();

        List<Tenant> tenants = tenantRepository.findAllByIdNotOrderByNameAsc(currentTenantId);
        List<PlatformSubscription> subscriptions = repository.findAllByTenantIdNotOrderByCreatedAtDesc(currentTenantId);

        Map<Long, PlatformSubscription> subscriptionsByTenantId = subscriptions.stream()
                .collect(Collectors.toMap(item -> item.getTenant().getId(), Function.identity(), (left, right) -> left));

        Map<Long, User> primaryUsersByTenantId = resolvePrimaryUsers(tenants.stream().map(Tenant::getId).toList());

        List<PlatformBillingTenantItemDTO> customers = tenants.stream()
                .map(tenant -> toTenantItem(
                        tenant,
                        subscriptionsByTenantId.get(tenant.getId()),
                        primaryUsersByTenantId.get(tenant.getId())
                ))
                .toList();

        BigDecimal projectedMonthlyRevenue = customers.stream()
                .filter(item -> "SUBSCRIPTION_REQUIRED".equals(item.billingMode()) && "ACTIVE".equals(item.status()))
                .map(item -> BigDecimal.valueOf(item.amount() == null ? 0D : item.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal pendingMonthlyRevenue = customers.stream()
                .filter(item -> "SUBSCRIPTION_REQUIRED".equals(item.billingMode()))
                .filter(item -> !"ACTIVE".equals(item.status()) && !"CANCELLED".equals(item.status()))
                .map(item -> BigDecimal.valueOf(item.amount() == null ? 0D : item.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<MercadoPagoSubscriptionItemDTO> mercadoPagoSubscriptions = buildMercadoPagoSubscriptions(subscriptions);

        BigDecimal mercadoPagoProjectedRevenue = mercadoPagoSubscriptions.stream()
                .filter(item -> "authorized".equalsIgnoreCase(item.status()) || "active".equalsIgnoreCase(item.status()))
                .map(item -> BigDecimal.valueOf(item.amount() == null ? 0D : item.amount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new PlatformBillingAdminOverviewResponseDTO(
                currentUserService.getCurrentUser().getTenant().getName(),
                customers.size(),
                (int) customers.stream()
                        .filter(item -> "SUBSCRIPTION_REQUIRED".equals(item.billingMode()) && "ACTIVE".equals(item.status()))
                        .count(),
                projectedMonthlyRevenue.doubleValue(),
                pendingMonthlyRevenue.doubleValue(),
                mercadoPagoSubscriptions.size(),
                (int) mercadoPagoSubscriptions.stream()
                        .filter(item -> "authorized".equalsIgnoreCase(item.status()) || "active".equalsIgnoreCase(item.status()))
                        .count(),
                mercadoPagoProjectedRevenue.doubleValue(),
                customers,
                mercadoPagoSubscriptions
        );
    }

    @Transactional
    public PlatformWorkspaceProvisionResponseDTO provisionWorkspace(PlatformWorkspaceProvisionRequestDTO dto) {
        currentUserService.ensureBillingAdminTenant();

        TenantBillingMode billingMode = parseBillingMode(dto.billingMode());
        String normalizedSlug = normalizeSlug(dto.tenantSlug());

        if (tenantRepository.existsBySlugIgnoreCase(normalizedSlug)) {
            throw new BusinessException("Ja existe um workspace com esse slug.");
        }
        if (userRepository.existsByEmail(dto.adminEmail())) {
            throw new BusinessException("Ja existe um usuario com esse e-mail.");
        }
        if (userRepository.existsByCpf(dto.adminCpf())) {
            throw new BusinessException("Ja existe um usuario com esse CPF.");
        }

        Tenant tenant = new Tenant();
        tenant.setName(dto.tenantName().trim());
        tenant.setSlug(normalizedSlug);
        tenant.setCnpj(StringUtils.hasText(dto.tenantCnpj()) ? dto.tenantCnpj().trim() : null);
        tenant.setActive(true);
        tenant.setBillingMode(billingMode);
        tenant = tenantRepository.save(tenant);

        User adminUser = new User();
        adminUser.setName(dto.adminName().trim());
        adminUser.setEmail(dto.adminEmail().trim().toLowerCase());
        adminUser.setCpf(dto.adminCpf().trim());
        adminUser.setPassword(passwordEncoder.encode(dto.adminPassword()));
        adminUser.setTenant(tenant);
        adminUser = userRepository.save(adminUser);

        String platformStatus = billingMode == TenantBillingMode.COMPLIMENTARY ? "ACTIVE" : "NOT_STARTED";
        String checkoutUrl = null;

        if (billingMode == TenantBillingMode.SUBSCRIPTION_REQUIRED && mercadoPagoService.isEnabled()) {
            PlatformSubscriptionResponseDTO checkout = platformSubscriptionService.createOrRefreshCheckoutForTenant(
                    tenant.getId(),
                    adminUser.getEmail()
            );
            platformStatus = checkout.status();
            checkoutUrl = checkout.checkoutUrl();
        }

        return new PlatformWorkspaceProvisionResponseDTO(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getBillingMode().name(),
                adminUser.getId(),
                adminUser.getName(),
                adminUser.getEmail(),
                platformStatus,
                checkoutUrl
        );
    }

    @Transactional(readOnly = true)
    public PlatformPaymentLinkResponseDTO createPaymentLink(PlatformPaymentLinkRequestDTO dto) {
        currentUserService.ensureBillingAdminTenant();

        Tenant linkedTenant = resolveLinkedTenant(dto.tenantId());
        String payerEmail = resolvePayerEmail(linkedTenant, dto.payerEmail());

        if (!StringUtils.hasText(payerEmail)) {
            throw new BusinessException("Informe um email pagador ou selecione um cliente com email configurado.");
        }

        String externalReference = buildExternalReference(linkedTenant);
        MercadoPagoService.PaymentLinkResult result = mercadoPagoService.createStandalonePaymentLink(
                dto.title(),
                dto.description(),
                dto.amount(),
                payerEmail,
                externalReference
        );

        return new PlatformPaymentLinkResponseDTO(
                result.externalReference(),
                result.preferenceId(),
                result.initPoint()
        );
    }

    private Map<Long, User> resolvePrimaryUsers(Collection<Long> tenantIds) {
        if (tenantIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, User> primaryUsersByTenantId = new LinkedHashMap<>();
        for (User user : userRepository.findAllByTenantIdInOrderByTenantIdAscIdAsc(tenantIds)) {
            primaryUsersByTenantId.putIfAbsent(user.getTenant().getId(), user);
        }

        return primaryUsersByTenantId;
    }

    private Tenant resolveLinkedTenant(Long tenantId) {
        if (tenantId == null) {
            return null;
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente SaaS nao encontrado para gerar link avulso."));

        if (tenant.getId().equals(currentUserService.getCurrentTenantId())) {
            throw new BusinessException("Selecione um cliente diferente do tenant interno da equipe.");
        }

        return tenant;
    }

    private String resolvePayerEmail(Tenant linkedTenant, String explicitEmail) {
        if (StringUtils.hasText(explicitEmail)) {
            return explicitEmail;
        }

        if (linkedTenant == null) {
            return null;
        }

        PlatformSubscription subscription = repository.findByTenantId(linkedTenant.getId()).orElse(null);
        if (subscription != null && StringUtils.hasText(subscription.getPayerEmail())) {
            return subscription.getPayerEmail();
        }

        return userRepository.findAllByTenantIdOrderByNameAsc(linkedTenant.getId())
                .stream()
                .map(User::getEmail)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String buildExternalReference(Tenant linkedTenant) {
        if (linkedTenant == null) {
            return "platform:manual:" + Instant.now().toEpochMilli();
        }

        return "platform:tenant:" + linkedTenant.getId() + ":manual:" + Instant.now().toEpochMilli();
    }

    private PlatformBillingTenantItemDTO toTenantItem(Tenant tenant, PlatformSubscription subscription, User primaryUser) {
        TenantBillingMode billingMode = tenant.getBillingMode();
        String status = resolveCustomerStatus(billingMode, subscription);
        String localPlanName = subscription != null
                ? subscription.getPlanName()
                : billingMode == TenantBillingMode.COMPLIMENTARY ? "Acesso cortesia" : planName;
        double amount = subscription != null
                ? subscription.getAmount().doubleValue()
                : billingMode == TenantBillingMode.COMPLIMENTARY ? 0D : monthlyPrice.doubleValue();
        String localCurrencyId = subscription != null ? subscription.getCurrencyId() : currencyId;
        String payerEmail = subscription != null && StringUtils.hasText(subscription.getPayerEmail())
                ? subscription.getPayerEmail()
                : primaryUser == null ? null : primaryUser.getEmail();
        String checkoutUrl = subscription == null ? null : subscription.getCheckoutUrl();
        LocalDate nextBillingDate = subscription == null ? null : subscription.getNextBillingDate();
        LocalDateTime lastPaymentAt = subscription == null ? null : subscription.getLastPaymentAt();

        return new PlatformBillingTenantItemDTO(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.isActive(),
                billingMode.name(),
                localPlanName,
                amount,
                localCurrencyId,
                status,
                primaryUser == null ? null : primaryUser.getName(),
                primaryUser == null ? null : primaryUser.getEmail(),
                payerEmail,
                subscription == null ? null : subscription.getExternalReference(),
                subscription == null ? null : subscription.getExternalSubscriptionId(),
                checkoutUrl,
                nextBillingDate,
                lastPaymentAt
        );
    }

    private String resolveCustomerStatus(TenantBillingMode billingMode, PlatformSubscription subscription) {
        if (billingMode == TenantBillingMode.COMPLIMENTARY) {
            return "COMPLIMENTARY";
        }

        if (subscription == null) {
            return "NOT_STARTED";
        }

        return subscription.getStatus().name();
    }

    private List<MercadoPagoSubscriptionItemDTO> buildMercadoPagoSubscriptions(List<PlatformSubscription> localSubscriptions) {
        if (!mercadoPagoService.isEnabled()) {
            return List.of();
        }

        Map<String, PlatformSubscription> subscriptionsByReference = localSubscriptions.stream()
                .filter(subscription -> StringUtils.hasText(subscription.getExternalReference()))
                .collect(Collectors.toMap(PlatformSubscription::getExternalReference, Function.identity(), (left, right) -> left));

        return mercadoPagoService.searchSubscriptions().stream()
                .map(item -> {
                    PlatformSubscription linkedSubscription = subscriptionsByReference.get(item.externalReference());

                    return new MercadoPagoSubscriptionItemDTO(
                            item.id(),
                            item.reason(),
                            item.status(),
                            item.payerEmail(),
                            item.externalReference(),
                            item.initPoint(),
                            item.amount() == null ? null : item.amount().doubleValue(),
                            item.currencyId(),
                            item.nextPaymentDate(),
                            linkedSubscription == null ? null : linkedSubscription.getTenant().getId(),
                            linkedSubscription == null ? null : linkedSubscription.getTenant().getName()
                    );
                })
                .toList();
    }

    private TenantBillingMode parseBillingMode(String rawBillingMode) {
        try {
            return TenantBillingMode.valueOf(rawBillingMode.trim().toUpperCase());
        } catch (Exception exception) {
            throw new BusinessException("Modo de acesso invalido para o workspace.");
        }
    }

    private String normalizeSlug(String rawSlug) {
        String normalized = Normalizer.normalize(rawSlug == null ? "" : rawSlug.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (!StringUtils.hasText(normalized)) {
            throw new BusinessException("Informe um slug valido para o workspace.");
        }

        return normalized;
    }
}
