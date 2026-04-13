package com.planno.dash_api.controller;

import com.planno.dash_api.dto.request.UserRequestDTO;
import com.planno.dash_api.dto.response.UserResponseDTO;
import com.planno.dash_api.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;

    @PostMapping
    public ResponseEntity<UserResponseDTO> criar(@RequestBody @Valid UserRequestDTO data) {
        return ResponseEntity.ok(service.salvar(data));
    }

    @GetMapping
    public ResponseEntity<List<UserResponseDTO>> listar() {
        return ResponseEntity.ok(service.listarUsuariosDoTenantAtual());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> me() {
        return ResponseEntity.ok(service.buscarUsuarioAtual());
    }
}
