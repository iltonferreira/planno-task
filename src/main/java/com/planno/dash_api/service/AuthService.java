package com.planno.dash_api.service;

import com.planno.dash_api.dto.mapper.AuthMapper;
import com.planno.dash_api.dto.request.LoginRequestDTO;
import com.planno.dash_api.dto.response.LoginResponseDTO;
import com.planno.dash_api.infra.TokenService;
import com.planno.dash_api.infra.exception.UnauthorizedException;
import com.planno.dash_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final UserService userService;
    private final AuthMapper authMapper;

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        var user = userRepository.findByEmail(dto.email())
                .orElseThrow(() -> new UnauthorizedException("Credenciais invalidas."));

        if (!passwordEncoder.matches(dto.password(), user.getPassword())) {
            throw new UnauthorizedException("Credenciais invalidas.");
        }

        return authMapper.toResponse(tokenService.generateToken(user), userService.buscarPorId(user.getId()));
    }
}
