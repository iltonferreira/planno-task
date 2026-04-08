package com.planno.dash_api.repository;

import com.planno.dash_api.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository // diz ao spring que essa classe lida com o banco de dados
public interface TenantRepository extends JpaRepository<Tenant, Long> {
    // Ao estender JpaRepository, o Spring cria AUTOMATICAMENTE para vocÃª:
    // .save() -> Salva ou atualiza
    // .findAll() -> Lista todos
    // .findById() -> Busca por ID
    // .deleteById() -> Deleta
    boolean existsBySlugIgnoreCase(String slug);

    List<Tenant> findAllByIdNotOrderByNameAsc(Long id);
}

