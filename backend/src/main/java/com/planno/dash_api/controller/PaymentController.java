package com.planno.dash_api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.planno.dash_api.dto.request.PaymentRequestDTO;
import com.planno.dash_api.dto.request.PaymentStatusUpdateRequestDTO;
import com.planno.dash_api.dto.response.PaymentResponseDTO;
import com.planno.dash_api.service.MercadoPagoWebhookSignatureService;
import com.planno.dash_api.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;
    private final MercadoPagoWebhookSignatureService webhookSignatureService;

    @GetMapping
    public ResponseEntity<List<PaymentResponseDTO>> getAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentResponseDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    public ResponseEntity<PaymentResponseDTO> create(@RequestBody @Valid PaymentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentResponseDTO> updateStatus(@PathVariable Long id, @RequestBody @Valid PaymentStatusUpdateRequestDTO dto) {
        return ResponseEntity.ok(service.updateStatus(id, dto.status()));
    }

    @PostMapping("/webhooks/mercado-pago")
    public ResponseEntity<Void> mercadoPagoWebhook(
            @RequestBody(required = false) JsonNode payload,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId
    ) {
        webhookSignatureService.validateOrThrow(dataId, requestId, signature);
        if (payload != null) {
            service.handleMercadoPagoWebhook(payload, dataId);
        }
        return ResponseEntity.accepted().build();
    }
}
