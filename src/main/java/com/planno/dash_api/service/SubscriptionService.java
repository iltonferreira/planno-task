package com.planno.dash_api.service;

import com.planno.dash_api.dto.mapper.SubscriptionMapper;
import com.planno.dash_api.dto.request.SubscriptionRequestDTO;
import com.planno.dash_api.dto.response.SubscriptionCheckoutResponseDTO;
import com.planno.dash_api.dto.response.SubscriptionResponseDTO;
import com.planno.dash_api.entity.Client;
import com.planno.dash_api.entity.Subscription;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.enums.SubscriptionStatus;
import com.planno.dash_api.infra.TenantContext;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.ClientRepository;
import com.planno.dash_api.repository.SubscriptionRepository;
import com.planno.dash_api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository repository;
    private final ClientRepository clientRepository;
    private final TenantRepository tenantRepository;
    private final SubscriptionMapper mapper;
    private final NotificationService notificationService;
    private final MercadoPagoService mercadoPagoService;

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> findAll() {
        Long tenantId = TenantContext.getTenantId();
        return repository.findAllByTenantId(tenantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SubscriptionResponseDTO> findByClientId(Long clientId) {
        Long tenantId = TenantContext.getTenantId();
        return repository.findByClientIdAndTenantId(clientId, tenantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public SubscriptionResponseDTO save(SubscriptionRequestDTO dto) {
        Long tenantId = TenantContext.getTenantId();

        Client client = clientRepository.findByIdAndTenantId(dto.clientId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado ou acesso negado."));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nao encontrado."));

        Subscription subscription = mapper.toEntity(dto);
        subscription.setClient(client);
        subscription.setTenant(tenant);

        return mapper.toResponse(repository.save(subscription));
    }

    @Transactional
    public SubscriptionResponseDTO updateStatus(Long id, SubscriptionStatus status) {
        Long tenantId = TenantContext.getTenantId();
        Subscription subscription = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Assinatura nao encontrada."));

        subscription.setStatus(status);
        Subscription saved = repository.save(subscription);
        notificationService.notifySubscriptionUpdated(saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public SubscriptionCheckoutResponseDTO createCheckout(Long id) {
        throw new BusinessException("O checkout recorrente do tenant foi desativado. Use Meu plano para a assinatura da plataforma.");
    }
}
