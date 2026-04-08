package com.planno.dash_api.entity;

import com.planno.dash_api.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "subscriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String description; // Ex: "GestÃ£o de TrÃ¡fego", "Hospedagem VIP"
    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;

    private LocalDate nextBillingDate; // PrÃ³ximo vencimento para controle do painel

    private String externalReference; // ID ou Link do Mercado Pago (Opcional por enquanto)

    private String externalSubscriptionId;

    @Column(length = 1000)
    private String checkoutUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;
}

