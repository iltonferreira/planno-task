package com.planno.dash_api.repository;

import com.planno.dash_api.entity.Subscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // Busca todas as assinaturas de um cliente especÃ­fico (respeitando o Tenant)
    @EntityGraph(attributePaths = "client")
    List<Subscription> findByClientIdAndTenantId(Long clientId, Long tenantId);

    @EntityGraph(attributePaths = "client")
    List<Subscription> findAllByTenantId(Long tenantId);

    @EntityGraph(attributePaths = "client")
    Optional<Subscription> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = "client")
    Optional<Subscription> findFirstByExternalSubscriptionId(String externalSubscriptionId);

    // Soma o valor de todas as assinaturas ATIVAS da agÃªncia (Sua visÃ£o 50/50)
    @Query("SELECT SUM(s.price) FROM Subscription s WHERE s.tenant.id = :tenantId AND s.status = 'ACTIVE'")
    BigDecimal sumActiveSubscriptionsByTenantId(Long tenantId);
}
