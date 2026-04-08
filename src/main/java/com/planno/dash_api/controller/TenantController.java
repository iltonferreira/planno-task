package com.planno.dash_api.controller;

import com.planno.dash_api.dto.request.TenantRequestDTO;
import com.planno.dash_api.dto.response.TenantResponseDTO;
import com.planno.dash_api.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController // 1. Diz que essa classe Ã© uma API que retorna JSON (nÃ£o uma pÃ¡gina HTML)
@RequestMapping("/api/tenants") // 2. Define que o "endereÃ§o" base dessa classe Ã© /api/tenants
public class TenantController {

    @Autowired // 3. Pede para o Spring entregar o Service pronto para uso
    private TenantService service;

    @PostMapping // 4. Mapeia requisiÃ§Ãµes do tipo POST (CriaÃ§Ã£o)
    public ResponseEntity<TenantResponseDTO> criar(@RequestBody @Valid TenantRequestDTO data) {
        // @RequestBody: Pega o JSON que vocÃª enviou no Postman e "derrete" ele dentro do DTO
        // @Valid: Manda o Spring conferir se as regras (como @NotBlank) estÃ£o sendo seguidas

        var response = service.salvar(data);

        // 5. Retorna o objeto criado com o status HTTP 200 (OK) ou 201 (Created)
        return ResponseEntity.ok(response);
    }

    @GetMapping // 6. Mapeia requisiÃ§Ãµes do tipo GET (Leitura)
    public ResponseEntity<List<TenantResponseDTO>> listar() {
        // 7. Chama o mÃ©todo de lista que a gente acabou de analisar no Service
        var lista = service.listarTodos();

        return ResponseEntity.ok(lista);
    }

    // BUSCAR UM ESPECÃFICO (GET com ID na URL)
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponseDTO> buscar(@PathVariable Long id) {
        var response = service.buscarPorId(id);
        return ResponseEntity.ok(response);
    }

    // ATUALIZAR (PUT)
    @PutMapping("/{id}")
    public ResponseEntity<TenantResponseDTO> atualizar(@PathVariable Long id, @RequestBody @Valid TenantRequestDTO data) {
        var response = service.atualizar(id, data);
        return ResponseEntity.ok(response);
    }

    // DELETAR (DELETE)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build(); // Retorna 204 (Sucesso sem conteÃºdo)
    }
}
