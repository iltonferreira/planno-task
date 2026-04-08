package com.planno.dash_api.repository;

import com.planno.dash_api.entity.Payment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @EntityGraph(attributePaths = {"client", "project", "subscription", "createdBy"})
    List<Payment> findAllByTenantIdOrderByCreatedAtDesc(Long tenantId);

    @EntityGraph(attributePaths = {"client", "project", "subscription", "createdBy"})
    Optional<Payment> findByIdAndTenantId(Long id, Long tenantId);

    @EntityGraph(attributePaths = {"client", "project", "subscription", "createdBy"})
    Optional<Payment> findFirstByExternalPaymentId(String externalPaymentId);

    @EntityGraph(attributePaths = {"client", "project", "subscription", "createdBy"})
    Optional<Payment> findFirstByExternalReferenceAndTenantId(String externalReference, Long tenantId);

    boolean existsByProjectIdAndTenantId(Long projectId, Long tenantId);
}
