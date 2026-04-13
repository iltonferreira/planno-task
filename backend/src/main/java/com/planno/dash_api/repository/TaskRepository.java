package com.planno.dash_api.repository;

import com.planno.dash_api.entity.Task;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"project", "responsibleUser", "createdBy", "participants"})
    List<Task> findAllByTenantId(Long tenantId);

    @EntityGraph(attributePaths = {"project", "responsibleUser", "createdBy", "participants"})
    List<Task> findAllByTenantIdAndResponsibleUserId(Long tenantId, Long responsibleUserId);

    @EntityGraph(attributePaths = {"project", "responsibleUser", "createdBy", "participants"})
    List<Task> findDistinctByTenantIdAndParticipantsId(Long tenantId, Long participantId);

    @EntityGraph(attributePaths = {"project", "responsibleUser", "createdBy", "participants"})
    Optional<Task> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByProjectIdAndTenantId(Long projectId, Long tenantId);

    @EntityGraph(attributePaths = {"project", "responsibleUser", "createdBy", "participants"})
    @Query("""
            select distinct t
            from Task t
            left join t.participants p
            where t.tenant.id = :tenantId
              and (t.responsibleUser.id = :userId or p.id = :userId or t.createdBy.id = :userId)
            """)
    List<Task> findVisibleByUserId(@Param("tenantId") Long tenantId, @Param("userId") Long userId);

    @EntityGraph(attributePaths = {"project", "responsibleUser", "createdBy", "participants"})
    @Query("""
            select distinct t
            from Task t
            left join t.participants p
            where t.tenant.id = :tenantId
              and t.id = :taskId
              and (t.responsibleUser.id = :userId or p.id = :userId or t.createdBy.id = :userId)
            """)
    Optional<Task> findVisibleByIdForUser(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("taskId") Long taskId
    );
}
