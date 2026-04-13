package com.planno.dash_api.controller;

import com.planno.dash_api.dto.response.GoogleCalendarAuthorizationUrlResponseDTO;
import com.planno.dash_api.dto.response.GoogleCalendarConnectionStatusResponseDTO;
import com.planno.dash_api.dto.response.GoogleCalendarEventResponseDTO;
import com.planno.dash_api.dto.response.GoogleCalendarTaskSyncResponseDTO;
import com.planno.dash_api.dto.response.TaskResponseDTO;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.service.GoogleCalendarIntegrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/integrations/google-calendar")
@RequiredArgsConstructor
public class GoogleCalendarIntegrationController {

    private final GoogleCalendarIntegrationService service;

    @GetMapping("/status")
    public ResponseEntity<GoogleCalendarConnectionStatusResponseDTO> status() {
        return ResponseEntity.ok(service.getStatus());
    }

    @PostMapping("/connect-url")
    public ResponseEntity<GoogleCalendarAuthorizationUrlResponseDTO> connectUrl() {
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

    @GetMapping("/events")
    public ResponseEntity<List<GoogleCalendarEventResponseDTO>> events(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        return ResponseEntity.ok(service.listEvents(start, end));
    }

    @PostMapping("/tasks/{taskId}/sync")
    public ResponseEntity<GoogleCalendarTaskSyncResponseDTO> syncTask(@PathVariable Long taskId) {
        return ResponseEntity.ok(service.syncTask(taskId));
    }

    @PostMapping("/import-event")
    public ResponseEntity<TaskResponseDTO> importEvent(
            @RequestParam(defaultValue = "primary") String calendarId,
            @RequestParam String eventId
    ) {
        return ResponseEntity.ok(service.importEvent(calendarId, eventId));
    }

    private ResponseEntity<Void> redirect(String location) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, URI.create(location).toString())
                .build();
    }
}
