package com.planno.dash_api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.planno.dash_api.dto.request.PlatformPaymentLinkRequestDTO;
import com.planno.dash_api.dto.request.PlatformWorkspaceProvisionRequestDTO;
import com.planno.dash_api.dto.response.PlatformBillingAdminOverviewResponseDTO;
import com.planno.dash_api.dto.response.PlatformPaymentLinkResponseDTO;
import com.planno.dash_api.dto.response.PlatformSubscriptionResponseDTO;
import com.planno.dash_api.dto.response.PlatformWorkspaceProvisionResponseDTO;
import com.planno.dash_api.service.MercadoPagoWebhookSignatureService;
import com.planno.dash_api.service.PlatformBillingAdminService;
import com.planno.dash_api.service.PlatformSubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/platform-billing")
@RequiredArgsConstructor
public class PlatformSubscriptionController {

    private final PlatformSubscriptionService service;
    private final PlatformBillingAdminService adminService;
    private final MercadoPagoWebhookSignatureService webhookSignatureService;

    @GetMapping("/me")
    public ResponseEntity<PlatformSubscriptionResponseDTO> me() {
        return ResponseEntity.ok(service.getCurrentTenantPlan());
    }

    @PostMapping("/checkout")
    public ResponseEntity<PlatformSubscriptionResponseDTO> createCheckout() {
        return ResponseEntity.ok(service.createOrRefreshCheckout());
    }

    @GetMapping("/admin/overview")
    public ResponseEntity<PlatformBillingAdminOverviewResponseDTO> getAdminOverview() {
        return ResponseEntity.ok(adminService.getOverview());
    }

    @PostMapping("/admin/customers")
    public ResponseEntity<PlatformWorkspaceProvisionResponseDTO> provisionWorkspace(
            @RequestBody @Valid PlatformWorkspaceProvisionRequestDTO dto
    ) {
        return ResponseEntity.ok(adminService.provisionWorkspace(dto));
    }

    @PostMapping("/admin/payment-links")
    public ResponseEntity<PlatformPaymentLinkResponseDTO> createPaymentLink(
            @RequestBody @Valid PlatformPaymentLinkRequestDTO dto
    ) {
        return ResponseEntity.ok(adminService.createPaymentLink(dto));
    }

    @PostMapping("/webhooks/mercado-pago")
    public ResponseEntity<Void> webhook(
            @RequestBody(required = false) JsonNode payload,
            @RequestParam(name = "data.id", required = false) String dataId,
            @RequestHeader(name = "x-signature", required = false) String signature,
            @RequestHeader(name = "x-request-id", required = false) String requestId
    ) {
        webhookSignatureService.validateOrThrow(dataId, requestId, signature);
        service.handleMercadoPagoWebhook(payload, dataId);
        return ResponseEntity.accepted().build();
    }
}
