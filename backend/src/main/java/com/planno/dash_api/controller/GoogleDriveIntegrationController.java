package com.planno.dash_api.controller;

import com.planno.dash_api.dto.response.GoogleDriveAuthorizationUrlResponseDTO;
import com.planno.dash_api.dto.response.GoogleDriveConnectionStatusResponseDTO;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.service.GoogleDriveIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/integrations/google-drive")
@RequiredArgsConstructor
public class GoogleDriveIntegrationController {

    private final GoogleDriveIntegrationService service;

    @GetMapping("/status")
    public ResponseEntity<GoogleDriveConnectionStatusResponseDTO> status() {
        return ResponseEntity.ok(service.getStatus());
    }

    @PostMapping("/connect-url")
    public ResponseEntity<GoogleDriveAuthorizationUrlResponseDTO> connectUrl() {
        return ResponseEntity.ok(service.createAuthorizationUrl());
    }

    @DeleteMapping("/connection")
    public ResponseEntity<Void> disconnect() {
        service.disconnect();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(
            @RequestParam String state,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String error
    ) {
        try {
            service.handleCallback(state, code, error);
            return redirect(service.buildFrontendSuccessRedirectUrl());
        } catch (BusinessException exception) {
            return redirect(service.buildFrontendErrorRedirectUrl(exception.getMessage()));
        }
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(location).toString())
                .build();
    }
}
