package com.planno.dash_api.entity;

import com.planno.dash_api.enums.PlatformSubscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlatformSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false, unique = true)
    private Tenant tenant;

    @Column(nullable = false, length = 100)
    private String planCode;

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 10)
    private String currencyId;

    private String payerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlatformSubscriptionStatus status = PlatformSubscriptionStatus.PENDING;

    @Column(unique = true)
    private String externalReference;

    private String externalSubscriptionId;

    @Column(length = 1000)
    private String checkoutUrl;

    private LocalDate nextBillingDate;

    private LocalDateTime lastPaymentAt;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = PlatformSubscriptionStatus.PENDING;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
