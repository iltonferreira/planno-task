package com.planno.dash_api.controller;

import com.planno.dash_api.dto.response.DashboardSummaryResponseDTO;
import com.planno.dash_api.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponseDTO> getSummary() {
        return ResponseEntity.ok(service.getSummary());
    }
}
