package com.planno.dash_api.controller;

import com.planno.dash_api.dto.request.KnowledgeBasePageRequestDTO;
import com.planno.dash_api.dto.response.KnowledgeBasePageResponseDTO;
import com.planno.dash_api.service.KnowledgeBasePageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-base")
@RequiredArgsConstructor
public class KnowledgeBasePageController {

    private final KnowledgeBasePageService service;

    @GetMapping
    public ResponseEntity<List<KnowledgeBasePageResponseDTO>> getAll(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.findAll(search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeBasePageResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<KnowledgeBasePageResponseDTO> create(@RequestBody @Valid KnowledgeBasePageRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<KnowledgeBasePageResponseDTO> update(@PathVariable Long id, @RequestBody @Valid KnowledgeBasePageRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
