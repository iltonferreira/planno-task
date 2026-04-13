package com.planno.dash_api.repository;

import com.planno.dash_api.entity.GoogleDriveConnection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GoogleDriveConnectionRepository extends JpaRepository<GoogleDriveConnection, Long> {

    Optional<GoogleDriveConnection> findByTenantId(Long tenantId);

    void deleteByTenantId(Long tenantId);
}
