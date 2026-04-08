package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.request.TaskRequestDTO;
import com.planno.dash_api.dto.response.TaskResponseDTO;
import com.planno.dash_api.entity.Task;
import com.planno.dash_api.enums.TaskPriority;
import com.planno.dash_api.enums.TaskStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
@RequiredArgsConstructor
public class TaskMapper {

    private final UserMapper userMapper;

    public Task toEntity(TaskRequestDTO dto) {
        Task task = new Task();
        applyUpdates(task, dto);
        return task;
    }

    public void applyUpdates(Task task, TaskRequestDTO dto) {
        task.setTitle(dto.title());
        task.setDescription(dto.description());
        task.setStatus(dto.status() == null ? TaskStatus.BACKLOG : dto.status());
        task.setPriority(dto.priority() == null ? TaskPriority.MEDIUM : dto.priority());
        task.setDueDate(dto.dueDate());
        task.setStartAt(dto.startAt());
        task.setEndAt(dto.endAt());
        task.setAllDay(dto.allDay() == null ? dto.startAt() == null && dto.endAt() == null : dto.allDay());
        task.setPositionIndex(dto.positionIndex() == null ? 0 : dto.positionIndex());
    }

    public TaskResponseDTO toResponse(Task task) {
        return new TaskResponseDTO(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus().name(),
                task.getPriority().name(),
                task.getDueDate(),
                task.getStartAt(),
                task.getEndAt(),
                task.isAllDay(),
                task.getPositionIndex(),
                task.getProject() == null ? null : task.getProject().getId(),
                task.getProject() == null ? null : task.getProject().getName(),
                userMapper.toSummary(task.getResponsibleUser()),
                userMapper.toSummary(task.getCreatedBy()),
                task.getParticipants().stream()
                        .sorted(Comparator.comparing(user -> user.getName() == null ? "" : user.getName()))
                        .map(userMapper::toSummary)
                        .toList(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
