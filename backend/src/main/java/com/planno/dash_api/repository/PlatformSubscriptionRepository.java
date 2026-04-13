package com.planno.dash_api.repository;

import com.planno.dash_api.entity.PlatformSubscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformSubscriptionRepository extends JpaRepository<PlatformSubscription, Long> {

    @EntityGraph(attributePaths = "tenant")
    Optional<PlatformSubscription> findByTenantId(Long tenantId);

    @EntityGraph(attributePaths = "tenant")
    Optional<PlatformSubscription> findFirstByExternalReference(String externalReference);

    @EntityGraph(attributePaths = "tenant")
    Optional<PlatformSubscription> findFirstByExternalSubscriptionId(String externalSubscriptionId);

    @EntityGraph(attributePaths = "tenant")
    List<PlatformSubscription> findAllByTenantIdNotOrderByCreatedAtDesc(Long tenantId);
}
