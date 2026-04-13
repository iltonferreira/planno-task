package com.planno.dash_api.repository;

import com.planno.dash_api.entity.DocumentAsset;
import com.planno.dash_api.enums.DocumentRelationType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentAssetRepository extends JpaRepository<DocumentAsset, Long> {

    @EntityGraph(attributePaths = {"uploadedBy"})
    List<DocumentAsset> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @EntityGraph(attributePaths = {"uploadedBy"})
    List<DocumentAsset> findAllByTenantIdAndRelationTypeAndRelationIdOrderByCreatedAtDesc(
            Long tenantId,
            DocumentRelationType relationType,
            Long relationId
    );

    @EntityGraph(attributePaths = {"uploadedBy"})
    Optional<DocumentAsset> findByIdAndTenantId(Long id, Long tenantId);
}
