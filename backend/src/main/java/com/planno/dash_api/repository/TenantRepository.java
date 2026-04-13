package com.planno.dash_api.repository;

import com.planno.dash_api.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    boolean existsBySlugIgnoreCase(String slug);

    List<Tenant> findAllByIdNotOrderByNameAsc(Long id);
}

