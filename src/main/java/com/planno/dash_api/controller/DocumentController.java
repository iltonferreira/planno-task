package com.planno.dash_api.controller;

import com.planno.dash_api.dto.response.DocumentResponseDTO;
import com.planno.dash_api.enums.DocumentRelationType;
import com.planno.dash_api.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService service;

    @GetMapping
    public ResponseEntity<List<DocumentResponseDTO>> getAll(
            @RequestParam(required = false) DocumentRelationType relationType,
            @RequestParam(required = false) Long relationId
    ) {
        return ResponseEntity.ok(service.findAll(relationType, relationId));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponseDTO> upload(
            @RequestParam DocumentRelationType relationType,
            @RequestParam(required = false) Long relationId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(service.upload(relationType, relationId, file));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        DocumentService.DownloadedDocument document = service.download(id);
        String contentType = (document.contentType() == null || document.contentType().isBlank())
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : document.contentType();
        String safeFileName = UriUtils.encode(document.fileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + safeFileName)
                .contentType(MediaType.parseMediaType(contentType))
                .body(document.content());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
