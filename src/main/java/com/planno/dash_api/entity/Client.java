package com.planno.dash_api.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clients")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class Client {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;
    private String phone;
    private String document; // NÃ£o obrigatÃ³rio

    private boolean active = true;

    // Controle de Soft Delete
    private LocalDateTime deletedAt;

    @ManyToOne
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    // Na classe Client.java adicione:
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL)
    private List<Subscription> subscriptions;

    @OneToMany(mappedBy = "client")
    private List<Payment> payments;



    // Campos virtuais para o Painel (nÃ£o salvos no banco, calculados no DTO)
    // - totalRevenue (Soma de Assinaturas + Avulsos)
    // - clientType (ASSINATURA, AVULSO, HIBRIDO)
}
