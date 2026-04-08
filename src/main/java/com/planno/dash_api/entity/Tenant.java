package com.planno.dash_api.entity;

import com.planno.dash_api.enums.TenantBillingMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;




@Entity
@Table(name = "tenants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true, nullable = false)
    private String slug;

    @Column(length = 14)
    private String cnpj;

    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantBillingMode billingMode = TenantBillingMode.SUBSCRIPTION_REQUIRED;

}

