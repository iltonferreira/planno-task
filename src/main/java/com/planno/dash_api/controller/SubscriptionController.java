package com.planno.dash_api.controller;

import com.planno.dash_api.dto.request.SubscriptionRequestDTO;
import com.planno.dash_api.dto.request.SubscriptionStatusUpdateRequestDTO;
import com.planno.dash_api.dto.response.SubscriptionCheckoutResponseDTO;
import com.planno.dash_api.dto.response.SubscriptionResponseDTO;
import com.planno.dash_api.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService service;

    @GetMapping
    public ResponseEntity<List<SubscriptionResponseDTO>> getAll(@RequestParam(required = false) Long clientId) {
        if (clientId != null) {
            return ResponseEntity.ok(service.findByClientId(clientId));
        }
        return ResponseEntity.ok(service.findAll());
    }

    @PostMapping
    public ResponseEntity<SubscriptionResponseDTO> create(@RequestBody @Valid SubscriptionRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SubscriptionResponseDTO> updateStatus(@PathVariable Long id, @RequestBody @Valid SubscriptionStatusUpdateRequestDTO dto) {
        return ResponseEntity.ok(service.updateStatus(id, dto.status()));
    }

    @PostMapping("/{id}/checkout")
    public ResponseEntity<SubscriptionCheckoutResponseDTO> createCheckout(@PathVariable Long id) {
        return ResponseEntity.ok(service.createCheckout(id));
    }
}
