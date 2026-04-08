package com.planno.dash_api.entity;

import com.planno.dash_api.enums.DocumentRelationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentAsset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "mime_type", nullable = false)
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "storage_file_id", nullable = false, unique = true)
    private String storageFileId;

    @Column(name = "storage_folder_id")
    private String storageFolderId;

    @Column(name = "storage_folder_path", nullable = false)
    private String storageFolderPath;

    @Column(name = "web_view_url", length = 1000)
    private String webViewUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    private DocumentRelationType relationType;

    @Column(name = "relation_id")
    private Long relationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by_user_id")
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
