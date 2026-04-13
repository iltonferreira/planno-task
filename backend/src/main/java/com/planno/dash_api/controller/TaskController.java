package com.planno.dash_api.controller;

import com.planno.dash_api.dto.request.TaskRequestDTO;
import com.planno.dash_api.dto.request.TaskStatusUpdateRequestDTO;
import com.planno.dash_api.dto.response.TaskResponseDTO;
import com.planno.dash_api.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService service;

    @GetMapping
    public ResponseEntity<List<TaskResponseDTO>> getAll(
            @RequestParam(required = false) Long responsibleUserId,
            @RequestParam(required = false) Long participantUserId
    ) {
        return ResponseEntity.ok(service.findAll(responsibleUserId, participantUserId));
    }

    @GetMapping("/me")
    public ResponseEntity<List<TaskResponseDTO>> myTasks() {
        return ResponseEntity.ok(service.findMyTasks());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<TaskResponseDTO> create(@RequestBody @Valid TaskRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponseDTO> update(@PathVariable Long id, @RequestBody @Valid TaskRequestDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponseDTO> updateStatus(@PathVariable Long id, @RequestBody @Valid TaskStatusUpdateRequestDTO dto) {
        return ResponseEntity.ok(service.updateStatus(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
