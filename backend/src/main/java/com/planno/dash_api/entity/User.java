package com.planno.dash_api.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String email;

    @Column(unique = true, nullable = false, length = 11)
    private String cpf;

    private String password;

    @ManyToOne
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;
}

