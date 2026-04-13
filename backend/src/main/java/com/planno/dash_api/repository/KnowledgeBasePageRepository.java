package com.planno.dash_api.repository;

import com.planno.dash_api.entity.KnowledgeBasePage;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeBasePageRepository extends JpaRepository<KnowledgeBasePage, Long> {

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    List<KnowledgeBasePage> findAllByTenantIdOrderByPinnedDescUpdatedAtDesc(Long tenantId);

    @EntityGraph(attributePaths = {"createdBy", "updatedBy"})
    Optional<KnowledgeBasePage> findByIdAndTenantId(Long id, Long tenantId);

    boolean existsByTenantIdAndSlug(Long tenantId, String slug);

    boolean existsByTenantIdAndSlugAndIdNot(Long tenantId, String slug, Long id);
}
