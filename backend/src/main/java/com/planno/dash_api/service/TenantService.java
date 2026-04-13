package com.planno.dash_api.service;

import com.planno.dash_api.dto.request.TenantRequestDTO;
import com.planno.dash_api.dto.response.TenantResponseDTO;
import com.planno.dash_api.dto.mapper.TenantMapper;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.enums.TenantBillingMode;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository repository;

    private final TenantMapper mapper;

    @Transactional
    public TenantResponseDTO salvar(TenantRequestDTO dto) {
        var entity = mapper.toEntity(dto);

        var salvo = repository.save(entity);

        return mapper.toResponseDTO(salvo);
    }

    public List<TenantResponseDTO> listarTodos(){
        return repository.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toUnmodifiableList());
    }

    public TenantResponseDTO buscarPorId(Long id){
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nao encontrado."));
    }

    @Transactional
    public TenantResponseDTO atualizar(Long id, TenantRequestDTO dto) {
        Tenant tenantExistence = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nao encontrado."));

        tenantExistence.setName(dto.name());
        tenantExistence.setSlug(dto.slug());
        tenantExistence.setCnpj(dto.cnpj());
        if (dto.billingMode() != null && !dto.billingMode().isBlank()) {
            tenantExistence.setBillingMode(TenantBillingMode.valueOf(dto.billingMode()));
        }

        Tenant salvo = repository.save(tenantExistence);
        return mapper.toResponseDTO(salvo);
    }

    @Transactional
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Tenant nao encontrado.");
        }
        repository.deleteById(id);
    }
}

