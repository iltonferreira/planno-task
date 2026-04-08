package com.planno.dash_api.repository;

import com.planno.dash_api.entity.GoogleDriveOAuthState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface GoogleDriveOAuthStateRepository extends JpaRepository<GoogleDriveOAuthState, Long> {

    Optional<GoogleDriveOAuthState> findByState(String state);

    void deleteByState(String state);

    void deleteByTenantId(Long tenantId);

    void deleteByExpiresAtBefore(LocalDateTime threshold);
}
