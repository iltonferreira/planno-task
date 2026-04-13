package com.planno.dash_api.service;

public interface StorageService {

    boolean isEnabled();

    StorageFolder ensureFolder(String folderPath);

    StoredFile upload(String folderId, String folderPath, String fileName, String contentType, byte[] content);

    StoredFileContent download(String fileId);

    void delete(String fileId);

    record StorageFolder(
            String folderId,
            String path
    ) {
    }

    record StoredFile(
            String fileId,
            String name,
            String mimeType,
            Long size,
            String webViewUrl,
            String folderId,
            String folderPath
    ) {
    }

    record StoredFileContent(
            String fileName,
            String contentType,
            byte[] content
    ) {
    }
}
