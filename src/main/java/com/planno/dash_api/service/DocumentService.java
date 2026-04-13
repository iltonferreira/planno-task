package com.planno.dash_api.service;

import com.planno.dash_api.dto.mapper.DocumentMapper;
import com.planno.dash_api.dto.response.DocumentResponseDTO;
import com.planno.dash_api.entity.DocumentAsset;
import com.planno.dash_api.enums.DocumentRelationType;
import com.planno.dash_api.infra.exception.BusinessException;
import com.planno.dash_api.infra.exception.ResourceNotFoundException;
import com.planno.dash_api.repository.ClientRepository;
import com.planno.dash_api.repository.DocumentAssetRepository;
import com.planno.dash_api.repository.KnowledgeBasePageRepository;
import com.planno.dash_api.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentAssetRepository repository;
    private final ClientRepository clientRepository;
    private final ProjectRepository projectRepository;
    private final KnowledgeBasePageRepository knowledgeBasePageRepository;
    private final CurrentUserService currentUserService;
    private final StorageService storageService;
    private final DocumentMapper mapper;

    @Value("${app.security.max-upload-size-bytes:10485760}")
    private long maxUploadSizeBytes;

    @Transactional(readOnly = true)
    public List<DocumentResponseDTO> findAll(DocumentRelationType relationType, Long relationId) {
        Long tenantId = currentUserService.getCurrentTenantId();
        List<DocumentAsset> documents;

        if (relationType != null && relationId != null) {
            documents = repository.findAllByTenantIdAndRelationTypeAndRelationIdOrderByCreatedAtDesc(tenantId, relationType, relationId);
        } else {
            documents = repository.findAllByTenantIdOrderByCreatedAtDesc(tenantId);
            if (relationType != null) {
                documents = documents.stream()
                        .filter(document -> document.getRelationType() == relationType)
                        .toList();
            }
        }

        return documents.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional
    public DocumentResponseDTO upload(DocumentRelationType relationType, Long relationId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Nenhum arquivo foi enviado.");
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new BusinessException("Arquivo excede o limite permitido.");
        }

        var currentUser = currentUserService.getCurrentUser();
        String folderPath = resolveFolderPath(currentUser.getTenant().getId(), relationType, relationId);

        try {
            String originalFileName = sanitizeFileName(file.getOriginalFilename());
            StorageService.StorageFolder folder = storageService.ensureFolder(folderPath);
            StorageService.StoredFile storedFile = storageService.upload(
                    folder.folderId(),
                    folder.path(),
                    originalFileName,
                    file.getContentType(),
                    file.getBytes()
            );

            DocumentAsset document = new DocumentAsset();
            document.setName(storedFile.name());
            document.setMimeType(storedFile.mimeType());
            document.setFileSize(storedFile.size());
            document.setStorageFileId(storedFile.fileId());
            document.setStorageFolderId(storedFile.folderId());
            document.setStorageFolderPath(storedFile.folderPath());
            document.setWebViewUrl(storedFile.webViewUrl());
            document.setRelationType(relationType);
            document.setRelationId(relationId);
            document.setUploadedBy(currentUser);
            document.setTenant(currentUser.getTenant());

            return mapper.toResponse(repository.save(document));
        } catch (IOException exception) {
            throw new BusinessException("Nao foi possivel ler o arquivo enviado.", exception);
        }
    }

    @Transactional(readOnly = true)
    public DownloadedDocument download(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        DocumentAsset document = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado."));

        StorageService.StoredFileContent content = storageService.download(document.getStorageFileId());
        return new DownloadedDocument(content.fileName(), content.contentType(), content.content());
    }

    @Transactional
    public void delete(Long id) {
        Long tenantId = currentUserService.getCurrentTenantId();
        DocumentAsset document = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado."));

        storageService.delete(document.getStorageFileId());
        repository.delete(document);
    }

    private String resolveFolderPath(Long tenantId, DocumentRelationType relationType, Long relationId) {
        return switch (relationType) {
            case CLIENT -> "/Clients/" + clientRepository.findByIdAndTenantId(requireRelationId(relationId), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Cliente nao encontrado para vincular documento."))
                    .getName();
            case PROJECT -> "/Projects/" + projectRepository.findByIdAndTenantId(requireRelationId(relationId), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Projeto nao encontrado para vincular documento."))
                    .getName();
            case KNOWLEDGE_BASE -> "/KnowledgeBase/" + knowledgeBasePageRepository.findByIdAndTenantId(requireRelationId(relationId), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Pagina nao encontrada para vincular documento."))
                    .getSlug();
            case FINANCE -> "/Finance";
            case GENERAL -> "/General";
        };
    }

    private Long requireRelationId(Long relationId) {
        if (relationId == null) {
            throw new BusinessException("Este tipo de documento exige um relationId.");
        }
        return relationId;
    }

    private String sanitizeFileName(String fileName) {
        String sanitized = StringUtils.hasText(fileName) ? fileName.trim() : "documento";
        sanitized = sanitized.replace("\\", "/");
        int lastSlash = sanitized.lastIndexOf('/');
        if (lastSlash >= 0) {
            sanitized = sanitized.substring(lastSlash + 1);
        }
        sanitized = sanitized.replaceAll("[\\r\\n\\t\\x00]", "_");
        return sanitized.isBlank() ? "documento" : sanitized;
    }

    public record DownloadedDocument(
            String fileName,
            String contentType,
            byte[] content
    ) {
    }
}
