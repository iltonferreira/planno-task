package com.planno.dash_api.repository;

import com.planno.dash_api.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    Optional<Project> findByIdAndTenantId(Long id, Long tenantId);
}
