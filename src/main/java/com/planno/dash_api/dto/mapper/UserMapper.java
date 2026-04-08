package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.request.UserRequestDTO;
import com.planno.dash_api.dto.response.UserResponseDTO;
import com.planno.dash_api.dto.response.UserSummaryResponseDTO;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public User toEntity(UserRequestDTO dto, Tenant tenant) {
        var user = new User();
        user.setName(dto.name());
        user.setEmail(dto.email());
        user.setPassword(dto.password());
        user.setCpf(dto.cpf());
        user.setTenant(tenant); // aqui eu seto o objeto completo que buscou no banco
        return user;
    }

    // Transforma a Entidade do banco para o DTO de resposta
    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCpf(),
                user.getTenant().getId(),   // Pega o ID de dentro do objeto Tenant
                user.getTenant().getName() // Pega o Nome de dentro do objeto Tenant
        );
    }

    public UserSummaryResponseDTO toSummary(User user) {
        if (user == null) {
            return null;
        }

        return new UserSummaryResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }
}
