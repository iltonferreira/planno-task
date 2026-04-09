package com.planno.dash_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planno.dash_api.dto.mapper.TaskMapper;
import com.planno.dash_api.dto.response.GoogleCalendarAuthorizationUrlResponseDTO;
import com.planno.dash_api.dto.response.GoogleCalendarConnectionStatusResponseDTO;
import com.planno.dash_api.dto.response.GoogleCalendarEventResponseDTO;
import com.planno.dash_api.dto.response.GoogleCalendarTaskSyncResponseDTO;
import com.planno.dash_api.dto.response.TaskResponseDTO;
import com.planno.dash_api.entity.GoogleCalendarConnection;
import com.planno.dash_api.entity.GoogleCalendarOAuthState;
import com.planno.dash_api.entity.Task;
import com.planno.dash_api.entity.TaskCalendarLink;
import com.planno.dash_api.entity.User;
import com.planno.dash_api.enums.TaskPriority;
import com.planno.dash_api.enums.TaskStatus;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.GoogleCalendarConnectionRepository;
import com.planno.dash_api.repository.GoogleCalendarOAuthStateRepository;
import com.planno.dash_api.repository.TaskCalendarLinkRepository;
import com.planno.dash_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GoogleCalendarIntegrationService {

    private static final DateTimeFormatter RFC_3339 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final GoogleCalendarConnectionRepository connectionRepository;
    private final GoogleCalendarOAuthStateRepository stateRepository;
    private final TaskCalendarLinkRepository taskCalendarLinkRepository;
    private final TaskRepository taskRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final TaskMapper taskMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.google-calendar.enabled:true}")
    private boolean enabled;

    @Value("${app.google-calendar.client-id:}")
    private String clientId;

    @Value("${app.google-calendar.client-secret:}")
    private String clientSecret;

    @Value("${app.google-calendar.redirect-uri:http://localhost:8080/api/integrations/google-calendar/callback}")
    private String redirectUri;

    @Value("${app.google-calendar.auth-base-url:https://accounts.google.com/o/oauth2/v2/auth}")
    private String authBaseUrl;

    @Value("${app.google-calendar.token-uri:https://oauth2.googleapis.com/token}")
    private String tokenUri;

    @Value("${app.google-calendar.userinfo-uri:https://openidconnect.googleapis.com/v1/userinfo}")
    private String userinfoUri;

    @Value("${app.google-calendar.revoke-uri:https://oauth2.googleapis.com/revoke}")
    private String revokeUri;

    @Value("${app.google-calendar.api-base-url:https://www.googleapis.com}")
    private String apiBaseUrl;

    @Value("${app.google-calendar.default-calendar-id:primary}")
    private String defaultCalendarId;

    @Value("${app.google-calendar.frontend-success-url:http://localhost:4200/calendar?googleCalendar=connected}")
    private String frontendSuccessUrl;

    @Value("${app.google-calendar.frontend-error-url:http://localhost:4200/calendar?googleCalendar=error}")
    private String frontendErrorUrl;

    @Value("${app.google-calendar.scopes:openid,email,profile,https://www.googleapis.com/auth/calendar}")
    private String scopes;

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(clientId)
                && StringUtils.hasText(clientSecret)
                && StringUtils.hasText(redirectUri);
    }

    @Transactional(readOnly = true)
    public GoogleCalendarConnectionStatusResponseDTO getStatus() {
        Long userId = currentUserService.getCurrentUser().getId();
        return connectionRepository.findByUserId(userId)
                .map(connection -> new GoogleCalendarConnectionStatusResponseDTO(
                        enabled,
                        isConfigured(),
                        true,
                        connection.getGoogleAccountEmail(),
                        resolveCalendarId(connection),
                        connection.getExpiresAt()
                ))
                .orElseGet(() -> new GoogleCalendarConnectionStatusResponseDTO(
                        enabled,
                        isConfigured(),
                        false,
                        null,
                        defaultCalendarId,
                        null
                ));
    }

    @Transactional
    public GoogleCalendarAuthorizationUrlResponseDTO createAuthorizationUrl() {
        if (!isConfigured()) {
            throw new BusinessException("Google Calendar OAuth ainda nao foi configurado no backend.");
        }

        User user = currentUserService.getCurrentUser();
        stateRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        stateRepository.deleteByUserId(user.getId());

        GoogleCalendarOAuthState state = new GoogleCalendarOAuthState();
        state.setUser(user);
        state.setState(UUID.randomUUID().toString());
        state.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        stateRepository.save(state);

        String scopeValue = urlEncode(String.join(" ", parseScopes()));
        String authorizationUrl = UriComponentsBuilder.fromUriString(authBaseUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", scopeValue)
                .queryParam("access_type", "offline")
                .queryParam("include_granted_scopes", "true")
                .queryParam("prompt", "consent")
                .queryParam("state", state.getState())
                .build(true)
                .toUriString();

        return new GoogleCalendarAuthorizationUrlResponseDTO(authorizationUrl);
    }

    @Transactional
    public void handleCallback(String state, String code, String error) {
        GoogleCalendarOAuthState oauthState = stateRepository.findByState(state)
                .orElseThrow(() -> new BusinessException("Estado OAuth do Google Calendar expirado ou invalido."));

        try {
            if (oauthState.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException("A autorizacao do Google Calendar expirou. Tente conectar novamente.");
            }
            if (StringUtils.hasText(error)) {
                throw new BusinessException("Google Calendar retornou erro na autorizacao: " + error);
            }
            if (!StringUtils.hasText(code)) {
                throw new BusinessException("Codigo de autorizacao do Google Calendar nao foi recebido.");
            }

            JsonNode tokenResponse = exchangeAuthorizationCode(code);
            GoogleCalendarConnection existing = connectionRepository.findByUserId(oauthState.getUser().getId()).orElse(null);
            String refreshToken = firstNonBlank(
                    tokenResponse.path("refresh_token").asText(null),
                    existing == null ? null : existing.getRefreshToken()
            );

            if (!StringUtils.hasText(refreshToken)) {
                throw new BusinessException("Google nao retornou refresh token para o Calendar. Refaca a conexao com consentimento.");
            }

            String accessToken = tokenResponse.path("access_token").asText(null);
            JsonNode profile = fetchUserProfile(accessToken);

            GoogleCalendarConnection connection = existing == null ? new GoogleCalendarConnection() : existing;
            connection.setUser(oauthState.getUser());
            connection.setGoogleAccountEmail(profile.path("email").asText(null));
            connection.setAccessToken(accessToken);
            connection.setRefreshToken(refreshToken);
            connection.setTokenType(tokenResponse.path("token_type").asText(null));
            connection.setScope(tokenResponse.path("scope").asText(null));
            connection.setExpiresAt(resolveExpiresAt(tokenResponse.path("expires_in").asLong(3600)));
            connection.setDefaultCalendarId(firstNonBlank(
                    existing == null ? null : existing.getDefaultCalendarId(),
                    defaultCalendarId
            ));
            connectionRepository.save(connection);
        } finally {
            stateRepository.deleteByState(state);
        }
    }

    @Transactional
    public void disconnect() {
        User currentUser = currentUserService.getCurrentUser();
        connectionRepository.findByUserId(currentUser.getId()).ifPresent(connection -> {
            revokeToken(firstNonBlank(connection.getRefreshToken(), connection.getAccessToken()));
            connectionRepository.delete(connection);
        });
        taskCalendarLinkRepository.deleteByUserId(currentUser.getId());
        stateRepository.deleteByUserId(currentUser.getId());
    }

    @Transactional
    public List<GoogleCalendarEventResponseDTO> listEvents(LocalDate start, LocalDate end) {
        if (end.isBefore(start)) {
            throw new BusinessException("O intervalo do Google Calendar e invalido.");
        }

        User currentUser = currentUserService.getCurrentUser();
        String calendarId = getDefaultCalendarId(currentUser.getId());
        JsonNode response = executeJsonRequest(
                HttpRequest.newBuilder()
                        .uri(URI.create(apiBaseUrl + "/calendar/v3/calendars/" + urlEncode(calendarId) + "/events"
                                + "?singleEvents=true&orderBy=startTime"
                                + "&timeMin=" + urlEncode(start.atStartOfDay().atOffset(ZoneOffset.UTC).format(RFC_3339))
                                + "&timeMax=" + urlEncode(end.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).format(RFC_3339))))
                        .header("Authorization", "Bearer " + getValidAccessToken(currentUser.getId()))
                        .GET()
                        .build()
        );

        Map<String, Long> linkedTasksByEventKey = taskCalendarLinkRepository.findAllByUserId(currentUser.getId()).stream()
                .collect(Collectors.toMap(
                        link -> buildEventKey(link.getCalendarId(), link.getExternalEventId()),
                        link -> link.getTask().getId(),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));

        if (!response.path("items").isArray()) {
            return List.of();
        }

        List<JsonNode> items = objectMapper.convertValue(
                response.path("items"),
                objectMapper.getTypeFactory().constructCollectionType(List.class, JsonNode.class)
        );

        return items.stream()
                .map(item -> toEventResponse(calendarId, item, linkedTasksByEventKey.get(buildEventKey(calendarId, item.path("id").asText(null)))))
                .toList();
    }

    @Transactional
    public GoogleCalendarTaskSyncResponseDTO syncTask(Long taskId) {
        User currentUser = currentUserService.getCurrentUser();
        Task task = taskRepository.findVisibleByIdForUser(currentUser.getTenant().getId(), currentUser.getId(), taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Tarefa nao encontrada para sincronizacao com Google Calendar."));

        if (task.getDueDate() == null && task.getStartAt() == null) {
            throw new BusinessException("A tarefa precisa ter um prazo ou horario para sincronizar com o Google Calendar.");
        }

        String calendarId = getDefaultCalendarId(currentUser.getId());
        TaskCalendarLink existingLink = taskCalendarLinkRepository.findByTaskIdAndUserId(taskId, currentUser.getId()).orElse(null);
        JsonNode response = executeJsonRequest(
                HttpRequest.newBuilder()
                        .uri(URI.create(buildEventWriteUri(calendarId, existingLink)))
                        .header("Authorization", "Bearer " + getValidAccessToken(currentUser.getId()))
                        .header("Content-Type", "application/json")
                        .method(existingLink == null ? "POST" : "PUT", HttpRequest.BodyPublishers.ofString(writeJson(buildEventPayload(task))))
                        .build()
        );

        TaskCalendarLink link = existingLink == null ? new TaskCalendarLink() : existingLink;
        link.setTask(task);
        link.setUser(currentUser);
        link.setCalendarId(calendarId);
        link.setExternalEventId(response.path("id").asText(null));
        link.setImportedFromGoogle(existingLink != null && existingLink.isImportedFromGoogle());
        link.setLastSyncedAt(LocalDateTime.now());
        taskCalendarLinkRepository.save(link);

        return new GoogleCalendarTaskSyncResponseDTO(
                task.getId(),
                calendarId,
                link.getExternalEventId(),
                response.path("htmlLink").asText(null),
                link.getLastSyncedAt()
        );
    }

    @Transactional
    public TaskResponseDTO importEvent(String calendarId, String eventId) {
        User currentUser = currentUserService.getCurrentUser();
        String effectiveCalendarId = StringUtils.hasText(calendarId) ? calendarId : getDefaultCalendarId(currentUser.getId());
        JsonNode event = executeJsonRequest(
                HttpRequest.newBuilder()
                        .uri(URI.create(apiBaseUrl + "/calendar/v3/calendars/" + urlEncode(effectiveCalendarId) + "/events/" + urlEncode(eventId)))
                        .header("Authorization", "Bearer " + getValidAccessToken(currentUser.getId()))
                        .GET()
                        .build()
        );

        TaskCalendarLink existingLink = taskCalendarLinkRepository.findByUserIdAndCalendarIdAndExternalEventId(
                currentUser.getId(), effectiveCalendarId, eventId
        ).orElse(null);

        Task task = existingLink == null ? new Task() : existingLink.getTask();
        if (existingLink == null) {
            task.setTenant(currentUser.getTenant());
            task.setCreatedBy(currentUser);
            task.setResponsibleUser(currentUser);
            task.setStatus(TaskStatus.BACKLOG);
            task.setPriority(TaskPriority.MEDIUM);
            task.setPositionIndex(nextPositionIndex(currentUser.getTenant().getId(), TaskStatus.BACKLOG));
        }

        applyGoogleEventToTask(task, event);
        Task savedTask = taskRepository.save(task);

        TaskCalendarLink link = existingLink == null ? new TaskCalendarLink() : existingLink;
        link.setTask(savedTask);
        link.setUser(currentUser);
        link.setCalendarId(effectiveCalendarId);
        link.setExternalEventId(eventId);
        link.setImportedFromGoogle(true);
        link.setLastSyncedAt(LocalDateTime.now());
        taskCalendarLinkRepository.save(link);

        return taskMapper.toResponse(savedTask);
    }

    public String buildFrontendSuccessRedirectUrl() {
        return frontendSuccessUrl;
    }

    public String buildFrontendErrorRedirectUrl(String message) {
        if (!StringUtils.hasText(message)) {
            return frontendErrorUrl;
        }

        return UriComponentsBuilder.fromUriString(frontendErrorUrl)
                .queryParam("message", message)
                .build(true)
                .toUriString();
    }

    @Transactional
    public String getValidAccessToken(Long userId) {
        if (!isConfigured()) {
            throw new BusinessException("Google Calendar OAuth ainda nao foi configurado no backend.");
        }

        GoogleCalendarConnection connection = connectionRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException("Google Calendar ainda nao foi conectado para este usuario."));

        if (StringUtils.hasText(connection.getAccessToken())
                && connection.getExpiresAt() != null
                && connection.getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(1))) {
            return connection.getAccessToken();
        }

        JsonNode refreshed = refreshAccessToken(connection.getRefreshToken());
        connection.setAccessToken(refreshed.path("access_token").asText(null));
        connection.setTokenType(refreshed.path("token_type").asText(connection.getTokenType()));
        connection.setScope(firstNonBlank(refreshed.path("scope").asText(null), connection.getScope()));
        connection.setExpiresAt(resolveExpiresAt(refreshed.path("expires_in").asLong(3600)));
        connectionRepository.save(connection);
        return connection.getAccessToken();
    }

    private String getDefaultCalendarId(Long userId) {
        return connectionRepository.findByUserId(userId)
                .map(this::resolveCalendarId)
                .orElse(defaultCalendarId);
    }

    private String buildEventWriteUri(String calendarId, TaskCalendarLink existingLink) {
        if (existingLink == null) {
            return apiBaseUrl + "/calendar/v3/calendars/" + urlEncode(calendarId) + "/events";
        }
        return apiBaseUrl + "/calendar/v3/calendars/" + urlEncode(calendarId) + "/events/" + urlEncode(existingLink.getExternalEventId());
    }

    private Map<String, Object> buildEventPayload(Task task) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", task.getTitle());
        payload.put("description", buildEventDescription(task));

        if (task.isAllDay() || task.getStartAt() == null) {
            LocalDate date = task.getDueDate() != null ? task.getDueDate() : task.getStartAt().toLocalDate();
            payload.put("start", Map.of("date", date.toString()));
            payload.put("end", Map.of("date", date.plusDays(1).toString()));
        } else {
            LocalDateTime startAt = task.getStartAt();
            LocalDateTime endAt = task.getEndAt() != null ? task.getEndAt() : task.getStartAt().plusHours(1);
            payload.put("start", Map.of("dateTime", startAt.atOffset(ZoneOffset.UTC).format(RFC_3339)));
            payload.put("end", Map.of("dateTime", endAt.atOffset(ZoneOffset.UTC).format(RFC_3339)));
        }

        return payload;
    }

    private String buildEventDescription(Task task) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(task.getDescription())) {
            builder.append(task.getDescription()).append("\n\n");
        }
        builder.append("Status: ").append(task.getStatus().name()).append('\n');
        builder.append("Prioridade: ").append(task.getPriority().name()).append('\n');
        if (task.getProject() != null) {
            builder.append("Projeto: ").append(task.getProject().getName()).append('\n');
        }
        if (task.getResponsibleUser() != null) {
            builder.append("Responsavel: ").append(task.getResponsibleUser().getName()).append('\n');
        }
        return builder.toString().trim();
    }

    private void applyGoogleEventToTask(Task task, JsonNode event) {
        task.setTitle(firstNonBlank(event.path("summary").asText(null), "Evento Google"));
        task.setDescription(firstNonBlank(event.path("description").asText(null), task.getDescription()));
        task.setStatus(task.getStatus() == null ? TaskStatus.BACKLOG : task.getStatus());
        task.setPriority(task.getPriority() == null ? TaskPriority.MEDIUM : task.getPriority());

        JsonNode start = event.path("start");
        JsonNode end = event.path("end");
        if (StringUtils.hasText(start.path("date").asText(null))) {
            LocalDate startDate = LocalDate.parse(start.path("date").asText());
            task.setDueDate(startDate);
            task.setStartAt(null);
            task.setEndAt(null);
            task.setAllDay(true);
            return;
        }

        LocalDateTime startAt = parseDateTime(start.path("dateTime").asText(null));
        LocalDateTime endAt = parseDateTime(end.path("dateTime").asText(null));
        task.setStartAt(startAt);
        task.setEndAt(endAt != null ? endAt : (startAt == null ? null : startAt.plusHours(1)));
        task.setDueDate(startAt == null ? task.getDueDate() : startAt.toLocalDate());
        task.setAllDay(false);
    }

    private GoogleCalendarEventResponseDTO toEventResponse(String calendarId, JsonNode event, Long linkedTaskId) {
        JsonNode start = event.path("start");
        JsonNode end = event.path("end");
        boolean allDay = StringUtils.hasText(start.path("date").asText(null));

        LocalDate startDate = allDay ? LocalDate.parse(start.path("date").asText()) : null;
        LocalDate endDate = allDay && StringUtils.hasText(end.path("date").asText(null))
                ? LocalDate.parse(end.path("date").asText()).minusDays(1)
                : startDate;

        return new GoogleCalendarEventResponseDTO(
                calendarId,
                event.path("id").asText(null),
                firstNonBlank(event.path("summary").asText(null), "Sem titulo"),
                event.path("description").asText(null),
                event.path("status").asText(null),
                event.path("htmlLink").asText(null),
                allDay,
                startDate,
                endDate,
                allDay ? null : parseDateTime(start.path("dateTime").asText(null)),
                allDay ? null : parseDateTime(end.path("dateTime").asText(null)),
                linkedTaskId
        );
    }

    private JsonNode exchangeAuthorizationCode(String code) {
        return executeFormRequest("grant_type=authorization_code"
                + "&code=" + urlEncode(code)
                + "&client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret)
                + "&redirect_uri=" + urlEncode(redirectUri));
    }

    private JsonNode refreshAccessToken(String refreshToken) {
        return executeFormRequest("grant_type=refresh_token"
                + "&refresh_token=" + urlEncode(refreshToken)
                + "&client_id=" + urlEncode(clientId)
                + "&client_secret=" + urlEncode(clientSecret));
    }

    private JsonNode executeFormRequest(String form) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(tokenUri))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Google OAuth Calendar respondeu com erro: " + response.body());
            }

            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException("Falha ao comunicar com o Google OAuth Calendar.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao comunicar com o Google OAuth Calendar.", exception);
        }
    }

    private JsonNode fetchUserProfile(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(userinfoUri))
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Nao foi possivel obter o perfil do Google Calendar.");
            }

            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException("Falha ao obter os dados do usuario do Google Calendar.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao obter os dados do usuario do Google Calendar.", exception);
        }
    }

    private JsonNode executeJsonRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Google Calendar respondeu com erro: " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException("Falha ao comunicar com o Google Calendar.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao comunicar com o Google Calendar.", exception);
        }
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new BusinessException("Falha ao serializar payload do Google Calendar.", exception);
        }
    }

    private void revokeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(revokeUri + "?token=" + urlEncode(token)))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();
            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception ignored) {
            // Revogacao best-effort.
        }
    }

    private LocalDateTime resolveExpiresAt(long expiresInSeconds) {
        return LocalDateTime.now().plusSeconds(Math.max(expiresInSeconds, 60));
    }

    private String resolveCalendarId(GoogleCalendarConnection connection) {
        return StringUtils.hasText(connection.getDefaultCalendarId()) ? connection.getDefaultCalendarId() : defaultCalendarId;
    }

    private List<String> parseScopes() {
        String normalizedScopes = scopes
                .replace("\"", " ")
                .replace("'", " ")
                .trim();

        return List.of(normalizedScopes.split("[,\\s]+")).stream()
                .map(String::trim)
                .filter(scope -> !scope.isBlank())
                .toList();
    }

    private LocalDateTime parseDateTime(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return OffsetDateTime.parse(value).toLocalDateTime();
    }

    private String buildEventKey(String calendarId, String eventId) {
        return calendarId + "::" + eventId;
    }

    private int nextPositionIndex(Long tenantId, TaskStatus status) {
        return (int) taskRepository.findAllByTenantId(tenantId).stream()
                .filter(task -> task.getStatus() == status)
                .count();
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}