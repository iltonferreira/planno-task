package com.planno.dash_api.repository;

import com.planno.dash_api.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClientRepository extends JpaRepository<Client, Long> {
    // Busca apenas clientes ativos no tenant logado
    List<Client> findAllByTenantIdAndActiveTrue(Long tenantId);

    // busca o cliente especifico garantino que pertenÃ§a ao tenant
    Optional<Client> findByIdAndTenantId(Long id, Long tenantId);

    //metodo para task de limpeza (120 dias)
    void deleteByActiveFalseAndDeletedAtBefore(LocalDateTime threshold);
}

