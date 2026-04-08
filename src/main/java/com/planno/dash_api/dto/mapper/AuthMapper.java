package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.response.LoginResponseDTO;
import com.planno.dash_api.dto.response.UserResponseDTO;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

    public LoginResponseDTO toResponse(String token, UserResponseDTO user) {
        return new LoginResponseDTO(token, user);
    }
}
