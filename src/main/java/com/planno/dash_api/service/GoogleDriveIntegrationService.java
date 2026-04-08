package com.planno.dash_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planno.dash_api.dto.response.GoogleDriveAuthorizationUrlResponseDTO;
import com.planno.dash_api.dto.response.GoogleDriveConnectionStatusResponseDTO;
import com.planno.dash_api.entity.GoogleDriveConnection;
import com.planno.dash_api.entity.GoogleDriveOAuthState;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.GoogleDriveConnectionRepository;
import com.planno.dash_api.repository.GoogleDriveOAuthStateRepository;
import com.planno.dash_api.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoogleDriveIntegrationService {

    private final GoogleDriveConnectionRepository connectionRepository;
    private final GoogleDriveOAuthStateRepository stateRepository;
    private final TenantRepository tenantRepository;
    private final CurrentUserService currentUserService;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.google-drive.enabled:true}")
    private boolean enabled;

    @Value("${app.google-drive.client-id:}")
    private String clientId;

    @Value("${app.google-drive.client-secret:}")
    private String clientSecret;

    @Value("${app.google-drive.redirect-uri:http://localhost:8080/api/integrations/google-drive/callback}")
    private String redirectUri;

    @Value("${app.google-drive.auth-base-url:https://accounts.google.com/o/oauth2/v2/auth}")
    private String authBaseUrl;

    @Value("${app.google-drive.token-uri:https://oauth2.googleapis.com/token}")
    private String tokenUri;

    @Value("${app.google-drive.userinfo-uri:https://openidconnect.googleapis.com/v1/userinfo}")
    private String userinfoUri;

    @Value("${app.google-drive.revoke-uri:https://oauth2.googleapis.com/revoke}")
    private String revokeUri;

    @Value("${app.google-drive.frontend-success-url:http://localhost:4200/documents?googleDrive=connected}")
    private String frontendSuccessUrl;

    @Value("${app.google-drive.frontend-error-url:http://localhost:4200/documents?googleDrive=error}")
    private String frontendErrorUrl;

    @Value("${app.google-drive.root-folder-id:root}")
    private String defaultRootFolderId;

    @Value("${app.google-drive.scopes:openid,email,profile,https://www.googleapis.com/auth/drive}")
    private String scopes;

    public boolean isConfigured() {
        return enabled
                && StringUtils.hasText(clientId)
                && StringUtils.hasText(clientSecret)
                && StringUtils.hasText(redirectUri);
    }

    @Transactional(readOnly = true)
    public boolean isEnabledForTenant(Long tenantId) {
        return isConfigured() && connectionRepository.findByTenantId(tenantId).isPresent();
    }

    @Transactional(readOnly = true)
    public GoogleDriveConnectionStatusResponseDTO getStatus() {
        Long tenantId = currentUserService.getCurrentTenantId();
        return connectionRepository.findByTenantId(tenantId)
                .map(connection -> new GoogleDriveConnectionStatusResponseDTO(
                        enabled,
                        isConfigured(),
                        true,
                        connection.getGoogleAccountEmail(),
                        resolveRootFolderId(connection),
                        connection.getExpiresAt()
                ))
                .orElseGet(() -> new GoogleDriveConnectionStatusResponseDTO(
                        enabled,
                        isConfigured(),
                        false,
                        null,
                        defaultRootFolderId,
                        null
                ));
    }

    @Transactional
    public GoogleDriveAuthorizationUrlResponseDTO createAuthorizationUrl() {
        if (!isConfigured()) {
            throw new BusinessException("Google Drive OAuth ainda nao foi configurado no backend.");
        }

        Long tenantId = currentUserService.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant nao encontrado para iniciar OAuth do Google Drive."));

        stateRepository.deleteByExpiresAtBefore(LocalDateTime.now());
        stateRepository.deleteByTenantId(tenantId);

        GoogleDriveOAuthState oauthState = new GoogleDriveOAuthState();
        oauthState.setState(UUID.randomUUID().toString());
        oauthState.setTenant(tenant);
        oauthState.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        stateRepository.save(oauthState);

        String authorizationUrl = UriComponentsBuilder.fromUriString(authBaseUrl)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("response_type", "code")
                .queryParam("scope", String.join(" ", parseScopes()))
                .queryParam("access_type", "offline")
                .queryParam("include_granted_scopes", "true")
                .queryParam("prompt", "consent")
                .queryParam("state", oauthState.getState())
                .build()
                .encode()
                .toUriString();

        return new GoogleDriveAuthorizationUrlResponseDTO(authorizationUrl);
    }

    @Transactional
    public void handleCallback(String state, String code, String error) {
        GoogleDriveOAuthState oauthState = stateRepository.findByState(state)
                .orElseThrow(() -> new BusinessException("Estado OAuth do Google Drive expirado ou invalido."));

        try {
            if (oauthState.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BusinessException("A autorizacao do Google Drive expirou. Tente conectar novamente.");
            }

            if (StringUtils.hasText(error)) {
                throw new BusinessException("Google Drive retornou erro na autorizacao: " + error);
            }

            if (!StringUtils.hasText(code)) {
                throw new BusinessException("Codigo de autorizacao do Google Drive nao foi recebido.");
            }

            JsonNode tokenResponse = exchangeAuthorizationCode(code);
            GoogleDriveConnection existingConnection = connectionRepository.findByTenantId(oauthState.getTenant().getId()).orElse(null);
            String refreshToken = firstNonBlank(
                    tokenResponse.path("refresh_token").asText(null),
                    existingConnection == null ? null : existingConnection.getRefreshToken()
            );

            if (!StringUtils.hasText(refreshToken)) {
                throw new BusinessException("Google nao retornou refresh token. Refaca a conexao com consentimento.");
            }

            String accessToken = tokenResponse.path("access_token").asText(null);
            JsonNode profile = fetchUserProfile(accessToken);

            GoogleDriveConnection connection = existingConnection == null ? new GoogleDriveConnection() : existingConnection;
            connection.setTenant(oauthState.getTenant());
            connection.setGoogleAccountEmail(profile.path("email").asText(null));
            connection.setAccessToken(accessToken);
            connection.setRefreshToken(refreshToken);
            connection.setTokenType(tokenResponse.path("token_type").asText(null));
            connection.setScope(tokenResponse.path("scope").asText(null));
            connection.setExpiresAt(resolveExpiresAt(tokenResponse.path("expires_in").asLong(3600)));
            connection.setRootFolderId(firstNonBlank(
                    existingConnection == null ? null : existingConnection.getRootFolderId(),
                    defaultRootFolderId
            ));

            connectionRepository.save(connection);
        } finally {
            stateRepository.deleteByState(state);
        }
    }

    @Transactional
    public void disconnect() {
        Long tenantId = currentUserService.getCurrentTenantId();
        connectionRepository.findByTenantId(tenantId).ifPresent(connection -> {
            revokeToken(firstNonBlank(connection.getRefreshToken(), connection.getAccessToken()));
            connectionRepository.delete(connection);
        });
        stateRepository.deleteByTenantId(tenantId);
    }

    @Transactional
    public String getValidAccessToken(Long tenantId) {
        if (!isConfigured()) {
            throw new BusinessException("Google Drive OAuth ainda nao foi configurado no backend.");
        }

        GoogleDriveConnection connection = connectionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new BusinessException("Google Drive ainda nao foi conectado para este tenant."));

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

    @Transactional(readOnly = true)
    public String getRootFolderId(Long tenantId) {
        return connectionRepository.findByTenantId(tenantId)
                .map(this::resolveRootFolderId)
                .orElse(defaultRootFolderId);
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
                throw new BusinessException("Google OAuth respondeu com erro: " + response.body());
            }

            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException("Falha ao comunicar com o Google OAuth.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao comunicar com o Google OAuth.", exception);
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
                throw new BusinessException("Nao foi possivel obter os dados do usuario do Google Drive.");
            }

            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException("Falha ao obter os dados do usuario do Google Drive.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao obter os dados do usuario do Google Drive.", exception);
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
            // Revogacao eh best-effort e nao deve impedir a desconexao local.
        }
    }

    private LocalDateTime resolveExpiresAt(long expiresInSeconds) {
        return LocalDateTime.now().plusSeconds(Math.max(expiresInSeconds, 60));
    }

    private String resolveRootFolderId(GoogleDriveConnection connection) {
        return StringUtils.hasText(connection.getRootFolderId()) ? connection.getRootFolderId() : defaultRootFolderId;
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

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : second;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
