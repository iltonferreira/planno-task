package com.planno.dash_api.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_calendar_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TaskCalendarLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "calendar_id", nullable = false)
    private String calendarId;

    @Column(name = "external_event_id", nullable = false)
    private String externalEventId;

    @Column(name = "imported_from_google", nullable = false)
    private boolean importedFromGoogle;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (calendarId == null || calendarId.isBlank()) {
            calendarId = "primary";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (calendarId == null || calendarId.isBlank()) {
            calendarId = "primary";
        }
    }
}
