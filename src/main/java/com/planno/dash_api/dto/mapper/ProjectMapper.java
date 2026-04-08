package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.request.ProjectRequestDTO;
import com.planno.dash_api.dto.response.ProjectResponseDTO;
import com.planno.dash_api.entity.Project;
import com.planno.dash_api.enums.ProjectStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectMapper {

    private final UserMapper userMapper;

    public Project toEntity(ProjectRequestDTO dto) {
        Project project = new Project();
        applyUpdates(project, dto);
        return project;
    }

    public void applyUpdates(Project project, ProjectRequestDTO dto) {
        project.setName(dto.name());
        project.setDescription(dto.description());
        project.setBudget(dto.budget());
        project.setStartDate(dto.startDate());
        project.setEndDate(dto.endDate());
        project.setStatus(dto.status() == null ? ProjectStatus.PLANNING : dto.status());
    }

    public ProjectResponseDTO toResponse(Project project) {
        return new ProjectResponseDTO(
                project.getId(),
                project.getName(),
                project.getDescription(),
                project.getStatus().name(),
                project.getBudget(),
                project.getStartDate(),
                project.getEndDate(),
                project.getClient() == null ? null : project.getClient().getId(),
                project.getClient() == null ? null : project.getClient().getName(),
                userMapper.toSummary(project.getOwnerUser()),
                userMapper.toSummary(project.getCreatedBy()),
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}
