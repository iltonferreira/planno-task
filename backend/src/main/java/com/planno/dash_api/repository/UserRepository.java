package com.planno.dash_api.repository;


import com.planno.dash_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    Optional<User> findByCpf(String cpf);

    boolean existsByEmail(String email);
    boolean existsByCpf(String cpf);

    List<User> findAllByTenantIdOrderByNameAsc(Long tenantId);

    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    List<User> findAllByIdInAndTenantId(Collection<Long> ids, Long tenantId);

    List<User> findAllByTenantIdInOrderByTenantIdAscIdAsc(Collection<Long> tenantIds);
}

