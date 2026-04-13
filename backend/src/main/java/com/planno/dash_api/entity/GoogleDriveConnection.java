package com.planno.dash_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "google_drive_connections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleDriveConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(name = "google_account_email", nullable = false)
    private String googleAccountEmail;

    @Column(name = "access_token", columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "refresh_token", columnDefinition = "TEXT", nullable = false)
    private String refreshToken;

    @Column(name = "token_type")
    private String tokenType;

    @Column(name = "scope", columnDefinition = "TEXT")
    private String scope;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "root_folder_id")
    private String rootFolderId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (rootFolderId == null || rootFolderId.isBlank()) {
            rootFolderId = "root";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (rootFolderId == null || rootFolderId.isBlank()) {
            rootFolderId = "root";
        }
    }
}
