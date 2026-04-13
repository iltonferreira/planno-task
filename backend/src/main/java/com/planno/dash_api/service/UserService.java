package com.planno.dash_api.service;

import com.planno.dash_api.dto.mapper.UserMapper;
import com.planno.dash_api.dto.request.UserRequestDTO;
import com.planno.dash_api.dto.response.UserResponseDTO;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.entity.User;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.TenantRepository;
import com.planno.dash_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository repository;
    private final TenantRepository tenantRepository;
    private final UserMapper mapper;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Transactional
    public UserResponseDTO salvar(UserRequestDTO dto) {
        if (repository.existsByEmail(dto.email())) {
            throw new BusinessException("E-mail ja cadastrado.");
        }
        if (repository.existsByCpf(dto.cpf())) {
            throw new BusinessException("CPF ja cadastrado.");
        }

        Tenant tenant = tenantRepository.findById(dto.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nao encontrado."));

        User novoUsuario = mapper.toEntity(dto, tenant);
        novoUsuario.setPassword(passwordEncoder.encode(dto.password()));

        User salvo = repository.save(novoUsuario);
        return mapper.toResponseDTO(salvo);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> listarUsuariosDoTenantAtual() {
        Long tenantId = currentUserService.getCurrentTenantId();
        return repository.findAllByTenantIdOrderByNameAsc(tenantId)
                .stream()
                .map(mapper::toResponseDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public UserResponseDTO buscarUsuarioAtual() {
        return mapper.toResponseDTO(currentUserService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO buscarPorId(Long id) {
        return repository.findById(id)
                .map(mapper::toResponseDTO)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado."));
    }

    @Transactional(readOnly = true)
    public User buscarEntidadePorIdDoTenant(Long id, Long tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado no tenant informado."));
    }
}
