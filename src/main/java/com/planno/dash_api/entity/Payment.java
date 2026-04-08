package com.planno.dash_api.entity;

import com.planno.dash_api.enums.PaymentDirection;
import com.planno.dash_api.enums.PaymentProvider;
import com.planno.dash_api.enums.PaymentStatus;
import com.planno.dash_api.enums.PaymentType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type = PaymentType.ONE_TIME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentDirection direction = PaymentDirection.INCOME;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider = PaymentProvider.MANUAL;

    private LocalDate dueDate;

    private LocalDateTime paidAt;

    @Column(name = "external_reference")
    private String externalReference;

    @Column(name = "external_payment_id")
    private String externalPaymentId;

    @Column(name = "external_preference_id")
    private String externalPreferenceId;

    @Column(name = "external_subscription_id")
    private String externalSubscriptionId;

    @Column(name = "checkout_url", length = 1000)
    private String checkoutUrl;

    @Column(name = "status_detail", length = 1000)
    private String statusDetail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id")
    private Subscription subscription;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

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
        if (type == null) {
            type = PaymentType.ONE_TIME;
        }
        if (direction == null) {
            direction = PaymentDirection.INCOME;
        }
        if (status == null) {
            status = PaymentStatus.PENDING;
        }
        if (provider == null) {
            provider = PaymentProvider.MANUAL;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
