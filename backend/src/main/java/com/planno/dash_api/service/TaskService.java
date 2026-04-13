package com.planno.dash_api.service;

import com.planno.dash_api.dto.mapper.TaskMapper;
import com.planno.dash_api.dto.request.TaskRequestDTO;
import com.planno.dash_api.dto.request.TaskStatusUpdateRequestDTO;
import com.planno.dash_api.dto.response.TaskResponseDTO;
import com.planno.dash_api.entity.Project;
import com.planno.dash_api.entity.Task;
import com.planno.dash_api.entity.User;
import com.planno.dash_api.enums.TaskStatus;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.ProjectRepository;
import com.planno.dash_api.repository.TaskRepository;
import com.planno.dash_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository repository;
    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final UserService userService;
    private final TaskMapper mapper;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> findAll(Long responsibleUserId, Long participantUserId) {
        Long tenantId = currentUserService.getCurrentTenantId();
        List<Task> tasks;

        if (responsibleUserId != null) {
            tasks = repository.findAllByTenantIdAndResponsibleUserId(tenantId, responsibleUserId);
        } else if (participantUserId != null) {
            tasks = repository.findDistinctByTenantIdAndParticipantsId(tenantId, participantUserId);
        } else {
            tasks = repository.findAllByTenantId(tenantId);
        }

        return toResponses(tasks);
    }

    @Transactional(readOnly = true)
    public List<TaskResponseDTO> findMyTasks() {
        var currentUser = currentUserService.getCurrentUser();
        return toResponses(repository.findVisibleByUserId(currentUser.getTenant().getId(), currentUser.getId()));
    }

    @Transactional(readOnly = true)
    public TaskResponseDTO findById(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        return mapper.toResponse(getTask(id, tenantId));
    }

    @Transactional
    public TaskResponseDTO save(TaskRequestDTO dto) {
        var currentUser = currentUserService.getCurrentUser();
        Long tenantId = currentUser.getTenant().getId();

        Task task = mapper.toEntity(dto);
        task.setTenant(currentUser.getTenant());
        task.setCreatedBy(currentUser);
        task.setProject(resolveProject(dto.projectId(), tenantId));
        task.setResponsibleUser(resolveUser(dto.responsibleUserId(), tenantId));
        task.setParticipants(resolveParticipants(dto.participantIds(), tenantId));
        normalizeSchedule(task);

        Task saved = repository.save(task);
        notifyResponsibleChange(null, saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public TaskResponseDTO update(Long id, TaskRequestDTO dto) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Task task = getTask(id, tenantId);
        User previousResponsible = task.getResponsibleUser();
        TaskStatus previousStatus = task.getStatus();

        mapper.applyUpdates(task, dto);
        task.setProject(resolveProject(dto.projectId(), tenantId));
        task.setResponsibleUser(resolveUser(dto.responsibleUserId(), tenantId));
        task.setParticipants(resolveParticipants(dto.participantIds(), tenantId));
        normalizeSchedule(task);

        Task saved = repository.save(task);
        notifyResponsibleChange(previousResponsible, saved);
        notifyStatusChange(previousStatus, saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public TaskResponseDTO updateStatus(Long id, TaskStatusUpdateRequestDTO dto) {
        Long tenantId = currentUserService.getCurrentTenantId();
        Task task = getTask(id, tenantId);
        TaskStatus previousStatus = task.getStatus();
        task.setStatus(dto.status());
        if (dto.positionIndex() != null) {
            task.setPositionIndex(dto.positionIndex());
        }

        Task saved = repository.save(task);
        notifyStatusChange(previousStatus, saved);
        return mapper.toResponse(saved);
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        repository.delete(getTask(id, tenantId));
    }

    private Task getTask(Long id, Long tenantId) {
        return repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa nao encontrada."));
    }

    private Project resolveProject(Long projectId, Long tenantId) {
        if (projectId == null) {
            return null;
        }

        return projectRepository.findByIdAndTenantId(projectId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Projeto nao encontrado para esta tarefa."));
    }

    private User resolveUser(Long userId, Long tenantId) {
        if (userId == null) {
            return null;
        }

        return userService.buscarEntidadePorIdDoTenant(userId, tenantId);
    }

    private LinkedHashSet<User> resolveParticipants(List<Long> participantIds, Long tenantId) {
        if (participantIds == null || participantIds.isEmpty()) {
            return new LinkedHashSet<>();
        }

        List<User> users = userRepository.findAllByIdInAndTenantId(participantIds, tenantId);
        if (users.size() != participantIds.size()) {
            throw new ResourceNotFoundException("Um ou mais participantes nao pertencem ao tenant informado.");
        }

        return new LinkedHashSet<>(users);
    }

    private void notifyResponsibleChange(User previousResponsible, Task task) {
        if (previousResponsible == null && task.getResponsibleUser() == null) {
            return;
        }

        if (previousResponsible == null || task.getResponsibleUser() == null
                || !previousResponsible.getId().equals(task.getResponsibleUser().getId())) {
            notificationService.notifyTaskAssigned(task);
        }
    }

    private void notifyStatusChange(TaskStatus previousStatus, Task task) {
        if (previousStatus != task.getStatus()) {
            notificationService.notifyTaskStatusChanged(task, previousStatus);
        }
    }

    private List<TaskResponseDTO> toResponses(List<Task> tasks) {
        return tasks.stream()
                .sorted(Comparator
                        .comparing((Task task) -> task.getStatus().ordinal())
                        .thenComparing(task -> task.getPositionIndex() == null ? 0 : task.getPositionIndex())
                        .thenComparing(Task::getCreatedAt))
                .map(mapper::toResponse)
                .toList();
    }

    private void normalizeSchedule(Task task) {
        if (task.isAllDay()) {
            if (task.getDueDate() == null && task.getStartAt() != null) {
                task.setDueDate(task.getStartAt().toLocalDate());
            }
            if (task.getDueDate() == null && task.getEndAt() != null) {
                task.setDueDate(task.getEndAt().toLocalDate());
            }
            task.setStartAt(null);
            task.setEndAt(null);
            return;
        }

        if (task.getStartAt() == null && task.getDueDate() != null) {
            task.setStartAt(task.getDueDate().atTime(9, 0));
        }
        if (task.getEndAt() == null && task.getStartAt() != null) {
            task.setEndAt(task.getStartAt().plusHours(1));
        }
        if (task.getStartAt() != null && task.getEndAt() != null && task.getEndAt().isBefore(task.getStartAt())) {
            throw new BusinessException("O horario final da tarefa nao pode ser anterior ao horario inicial.");
        }
        if (task.getDueDate() == null && task.getStartAt() != null) {
            task.setDueDate(task.getStartAt().toLocalDate());
        }
    }
}
