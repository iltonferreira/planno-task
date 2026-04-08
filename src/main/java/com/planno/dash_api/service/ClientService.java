package com.planno.dash_api.service;

import com.planno.dash_api.dto.request.ClientRequestDTO;
import com.planno.dash_api.dto.response.ClientResponseDTO;
import com.planno.dash_api.dto.mapper.ClientMapper;
import com.planno.dash_api.entity.Client;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.infra.TenantContext;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.ClientRepository;
import com.planno.dash_api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository repository;
    private final TenantRepository tenantRepository;
    private final ClientMapper mapper;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<ClientResponseDTO> findAll() {
        Long tenantId = TenantContext.getTenantId();
        return repository.findAllByTenantIdAndActiveTrue(tenantId)
                .stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ClientResponseDTO save(ClientRequestDTO dto) {
        Long tenantId = TenantContext.getTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nao encontrado."));

        Client client = mapper.toEntity(dto);
        client.setTenant(tenant);

        Client saved = repository.save(client);
        storageService.ensureFolder("/Clients/" + saved.getName());
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = TenantContext.getTenantId();
        // Garante que vocÃª sÃ³ deleta um cliente que pertence Ã  sua agÃªncia
        Client client = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado ou acesso negado."));

        client.setActive(false);
        client.setDeletedAt(LocalDateTime.now());
        repository.save(client);
    }
}
