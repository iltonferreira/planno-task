package com.planno.dash_api.service;

import com.planno.dash_api.dto.mapper.ProjectMapper;
import com.planno.dash_api.dto.request.ProjectRequestDTO;
import com.planno.dash_api.dto.response.ProjectResponseDTO;
import com.planno.dash_api.entity.Client;
import com.planno.dash_api.entity.Project;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.ClientRepository;
import com.planno.dash_api.repository.PaymentRepository;
import com.planno.dash_api.repository.ProjectRepository;
import com.planno.dash_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository repository;
    private final ClientRepository clientRepository;
    private final TaskRepository taskRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final ProjectMapper mapper;
    private final NotificationService notificationService;
    private final StorageService storageService;

    @Transactional(readOnly = true)
    public List<ProjectResponseDTO> findAll() {
        Long tenantId = currentUserService.getCurrentTenantId();
        return repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponseDTO findById(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        return mapper.toResponse(getProject(id, tenantId));
    }

    @Transactional
    public ProjectResponseDTO save(ProjectRequestDTO dto) {
        var currentUser = currentUserService.getCurrentUser();
        Project project = mapper.toEntity(dto);
        project.setTenant(currentUser.getTenant());
        project.setCreatedBy(currentUser);
        project.setClient(resolveClient(dto.clientId(), currentUser.getTenant().getId()));
        project.setOwnerUser(dto.ownerUserId() == null ? null : userService.buscarEntidadePorIdDoTenant(dto.ownerUserId(), currentUser.getTenant().getId()));

        Project saved = repository.save(project);
        storageService.ensureFolder("/Projects/" + saved.getName());
        notificationService.notifyProjectCreated(saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public ProjectResponseDTO update(Long id, ProjectRequestDTO dto) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Project project = getProject(id, tenantId);
        mapper.applyUpdates(project, dto);
        project.setClient(resolveClient(dto.clientId(), tenantId));
        project.setOwnerUser(dto.ownerUserId() == null ? null : userService.buscarEntidadePorIdDoTenant(dto.ownerUserId(), tenantId));
        return mapper.toResponse(repository.save(project));
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Project project = getProject(id, tenantId);

        if (taskRepository.existsByProjectIdAndTenantId(id, tenantId)) {
            throw new BusinessException("Nao e possivel excluir o projeto enquanto houver tarefas vinculadas.");
        }

        if (paymentRepository.existsByProjectIdAndTenantId(id, tenantId)) {
            throw new BusinessException("Nao e possivel excluir o projeto enquanto houver pagamentos vinculados.");
        }

        repository.delete(project);
    }

    private Project getProject(Long id, Long tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto nao encontrado."));
    }

    private Client resolveClient(Long clientId, Long tenantId) {
        if (clientId == null) {
            return null;
        }

        return clientRepository.findByIdAndTenantId(clientId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado para este tenant."));
    }
}
