package com.planno.dash_api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.planno.dash_api.infra.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GoogleDriveStorageService implements StorageService {

    private final ObjectMapper objectMapper;
    private final GoogleDriveIntegrationService integrationService;
    private final CurrentUserService currentUserService;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${app.google-drive.api-base-url:https://www.googleapis.com}")
    private String apiBaseUrl;

    @Override
    public boolean isEnabled() {
        try {
            Long tenantId = currentUserService.getCurrentTenantId();
            return integrationService.isEnabledForTenant(tenantId);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public StorageFolder ensureFolder(String folderPath) {
        String normalizedPath = normalizePath(folderPath);
        if (!isEnabled()) {
            return new StorageFolder(null, normalizedPath);
        }

        Long tenantId = currentUserService.getCurrentTenantId();
        List<String> segments = splitPath(normalizedPath);
        String parentId = integrationService.getRootFolderId(tenantId);

        for (String segment : segments) {
            String existingId = findFolderId(segment, parentId, tenantId);
            if (existingId != null) {
                parentId = existingId;
                continue;
            }

            parentId = createFolder(segment, parentId, tenantId);
        }

        return new StorageFolder(parentId, normalizedPath);
    }

    @Override
    public StoredFile upload(String folderId, String folderPath, String fileName, String contentType, byte[] content) {
        requireEnabled();
        Long tenantId = currentUserService.getCurrentTenantId();
        StorageFolder folder = StringUtils.hasText(folderId) ? new StorageFolder(folderId, normalizePath(folderPath)) : ensureFolder(folderPath);

        try {
            String boundary = "drive-upload-" + System.currentTimeMillis();
            Map<String, Object> metadata = new LinkedHashMap<>();
            metadata.put("name", fileName);
            metadata.put("parents", List.of(folder.folderId()));

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write("Content-Type: application/json; charset=UTF-8\r\n\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(objectMapper.writeValueAsBytes(metadata));
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(("Content-Type: " + (StringUtils.hasText(contentType) ? contentType : "application/octet-stream") + "\r\n\r\n")
                    .getBytes(StandardCharsets.UTF_8));
            output.write(content);
            output.write("\r\n".getBytes(StandardCharsets.UTF_8));
            output.write(("--" + boundary + "--").getBytes(StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/upload/drive/v3/files?uploadType=multipart&supportsAllDrives=true&fields=id,name,mimeType,webViewLink,size,parents"))
                    .header("Authorization", "Bearer " + integrationService.getValidAccessToken(tenantId))
                    .header("Content-Type", "multipart/related; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(output.toByteArray()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Falha ao enviar arquivo para o Google Drive: " + response.body());
            }

            JsonNode body = objectMapper.readTree(response.body());
            return new StoredFile(
                    body.path("id").asText(null),
                    body.path("name").asText(fileName),
                    body.path("mimeType").asText(contentType),
                    body.path("size").asLong(content.length),
                    body.path("webViewLink").asText(null),
                    folder.folderId(),
                    folder.path()
            );
        } catch (IOException exception) {
            throw new BusinessException("Falha ao enviar arquivo para o Google Drive.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao enviar arquivo para o Google Drive.", exception);
        }
    }

    @Override
    public StoredFileContent download(String fileId) {
        requireEnabled();
        Long tenantId = currentUserService.getCurrentTenantId();

        try {
            JsonNode metadata = executeJsonRequest(
                    HttpRequest.newBuilder()
                            .uri(URI.create(apiBaseUrl + "/drive/v3/files/" + urlEncode(fileId) + "?fields=id,name,mimeType&supportsAllDrives=true"))
                            .header("Authorization", "Bearer " + integrationService.getValidAccessToken(tenantId))
                            .GET()
                            .build()
            );

            HttpRequest contentRequest = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/drive/v3/files/" + urlEncode(fileId) + "?alt=media&supportsAllDrives=true"))
                    .header("Authorization", "Bearer " + integrationService.getValidAccessToken(tenantId))
                    .GET()
                    .build();

            HttpResponse<byte[]> contentResponse = httpClient.send(contentRequest, HttpResponse.BodyHandlers.ofByteArray());
            if (contentResponse.statusCode() >= 400) {
                throw new BusinessException("Falha ao baixar arquivo do Google Drive.");
            }

            return new StoredFileContent(
                    metadata.path("name").asText(fileId),
                    metadata.path("mimeType").asText("application/octet-stream"),
                    contentResponse.body()
            );
        } catch (IOException exception) {
            throw new BusinessException("Falha ao baixar arquivo do Google Drive.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao baixar arquivo do Google Drive.", exception);
        }
    }

    @Override
    public void delete(String fileId) {
        requireEnabled();
        Long tenantId = currentUserService.getCurrentTenantId();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/drive/v3/files/" + urlEncode(fileId) + "?supportsAllDrives=true"))
                    .header("Authorization", "Bearer " + integrationService.getValidAccessToken(tenantId))
                    .DELETE()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Falha ao remover arquivo do Google Drive: " + response.body());
            }
        } catch (IOException exception) {
            throw new BusinessException("Falha ao remover arquivo do Google Drive.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao remover arquivo do Google Drive.", exception);
        }
    }

    private String findFolderId(String folderName, String parentId, Long tenantId) {
        String query = "mimeType = 'application/vnd.google-apps.folder' and trashed = false and name = '"
                + folderName.replace("'", "\\'")
                + "' and '"
                + parentId
                + "' in parents";

        JsonNode body = executeJsonRequest(
                HttpRequest.newBuilder()
                        .uri(URI.create(apiBaseUrl + "/drive/v3/files?q=" + urlEncode(query)
                                + "&fields=files(id,name)&supportsAllDrives=true&includeItemsFromAllDrives=true"))
                        .header("Authorization", "Bearer " + integrationService.getValidAccessToken(tenantId))
                        .GET()
                        .build()
        );

        JsonNode files = body.path("files");
        if (!files.isArray() || files.isEmpty()) {
            return null;
        }

        return files.get(0).path("id").asText(null);
    }

    private String createFolder(String folderName, String parentId, Long tenantId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", folderName);
        payload.put("mimeType", "application/vnd.google-apps.folder");
        payload.put("parents", List.of(parentId));

        JsonNode body = executeJsonRequest(
                HttpRequest.newBuilder()
                        .uri(URI.create(apiBaseUrl + "/drive/v3/files?supportsAllDrives=true&fields=id,name"))
                        .header("Authorization", "Bearer " + integrationService.getValidAccessToken(tenantId))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(writeJson(payload)))
                        .build()
        );

        return body.path("id").asText(null);
    }

    private JsonNode executeJsonRequest(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException("Google Drive respondeu com erro: " + response.body());
            }
            return objectMapper.readTree(response.body());
        } catch (IOException exception) {
            throw new BusinessException("Falha ao comunicar com o Google Drive.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException("Falha ao comunicar com o Google Drive.", exception);
        }
    }

    private String writeJson(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (IOException exception) {
            throw new BusinessException("Falha ao serializar payload do Google Drive.", exception);
        }
    }

    private String normalizePath(String folderPath) {
        if (!StringUtils.hasText(folderPath)) {
            return "/General";
        }

        String normalized = folderPath.trim().replace("\\", "/");
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.replaceAll("/{2,}", "/");
    }

    private List<String> splitPath(String folderPath) {
        String normalized = normalizePath(folderPath).substring(1);
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> segments = new ArrayList<>();
        for (String segment : normalized.split("/")) {
            if (!segment.isBlank()) {
                segments.add(segment.trim());
            }
        }
        return segments;
    }

    private void requireEnabled() {
        if (!isEnabled()) {
            throw new BusinessException("Google Drive OAuth nao configurado ou ainda nao conectado para este tenant.");
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
