package com.planno.dash_api.repository;

import com.planno.dash_api.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findAllByTenantIdAndActiveTrue(Long tenantId);

    Optional<Client> findByIdAndTenantId(Long id, Long tenantId);

    void deleteByActiveFalseAndDeletedAtBefore(LocalDateTime threshold);
}

