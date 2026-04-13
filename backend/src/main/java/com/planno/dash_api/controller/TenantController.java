package com.planno.dash_api.controller;

import com.planno.dash_api.dto.request.TenantRequestDTO;
import com.planno.dash_api.dto.response.TenantResponseDTO;
import com.planno.dash_api.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService service;

    @PostMapping
    public ResponseEntity<TenantResponseDTO> criar(@RequestBody @Valid TenantRequestDTO data) {
        var response = service.salvar(data);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<TenantResponseDTO>> listar() {
        var lista = service.listarTodos();

        return ResponseEntity.ok(lista);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TenantResponseDTO> buscar(@PathVariable Long id) {
        var response = service.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TenantResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid TenantRequestDTO data) {
        var response = service.atualizar(id, data);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
