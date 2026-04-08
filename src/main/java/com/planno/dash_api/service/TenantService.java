package com.planno.dash_api.service;

import com.planno.dash_api.dto.request.TenantRequestDTO;
import com.planno.dash_api.dto.response.TenantResponseDTO;
import com.planno.dash_api.dto.mapper.TenantMapper;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.enums.TenantBillingMode;
import com.planno.dash_api.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
public class TenantService {

    @Autowired // o spring injeta o acesso ao banco aqui
    private TenantRepository repository;


    @Autowired // o spring injeta o "tradutor" aqui
    private TenantMapper mapper;


    @Transactional // se o banco de dados falhar, ele cancela tudo para nÃ£o salvar lixo
    public TenantResponseDTO salvar(TenantRequestDTO dto) {
        // 1. usa o mapper para transformar o DTO(JSON) em Entity(Banco)
        var entity = mapper.toEntity(dto);

        // 2. manda o respostory salvar no banco
        var salvo = repository.save(entity);

        // 3. transforma a Entity salva de volta para DTO para responder ao usuario
        return mapper.toResponseDTO(salvo);
    }

    public List<TenantResponseDTO> listarTodos(){
        // pega todos do banco e mapeia cada um para o formato resposta DTO
        return repository.findAll()
                .stream()
                .map(mapper::toResponseDTO)
                .collect(Collectors.toUnmodifiableList());
    }

    public TenantResponseDTO buscarPorId(Long id){
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new RuntimeException("Tenant nÃ£o encontrado"));
    }

    @Transactional // se o banco falhar, ele cancela tudo
    public TenantResponseDTO atualizar(Long id, TenantRequestDTO dto) {
        // busca tenant no banco primeiro
        Tenant tenantExistence = repository.findById(id).orElseThrow(()-> new RuntimeException("Tenant nÃ£o encontrado"));

        // atualiza os campos com o que veio do DTO
        tenantExistence.setName(dto.name());
        tenantExistence.setSlug(dto.slug());
        tenantExistence.setCnpj(dto.cnpj());
        if (dto.billingMode() != null && !dto.billingMode().isBlank()) {
            tenantExistence.setBillingMode(TenantBillingMode.valueOf(dto.billingMode()));
        }

        // salva novamente ( o JPA entende que Ã© um update porque o ID jÃ¡ existe)
        Tenant salvo = repository.save(tenantExistence);
        return mapper.toResponseDTO(salvo);
    }

    // 3. DELETAR
    @Transactional
    public void deletar(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("NÃ£o Ã© possÃ­vel deletar: Tenant nÃ£o encontrado");
        }
        repository.deleteById(id);
    }
}

