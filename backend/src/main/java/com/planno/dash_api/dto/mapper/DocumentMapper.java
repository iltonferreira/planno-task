package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.response.DocumentResponseDTO;
import com.planno.dash_api.entity.DocumentAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentMapper {

    private final UserMapper userMapper;

    public DocumentResponseDTO toResponse(DocumentAsset document) {
        return new DocumentResponseDTO(
                document.getId(),
                document.getName(),
                document.getMimeType(),
                document.getFileSize(),
                document.getStorageFileId(),
                document.getStorageFolderId(),
                document.getStorageFolderPath(),
                document.getWebViewUrl(),
                document.getRelationType().name(),
                document.getRelationId(),
                userMapper.toSummary(document.getUploadedBy()),
                document.getCreatedAt()
        );
    }
}
