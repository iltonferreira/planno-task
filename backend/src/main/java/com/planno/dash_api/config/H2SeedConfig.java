package com.planno.dash_api.config;

import com.planno.dash_api.entity.PlatformSubscription;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.entity.User;
import com.planno.dash_api.enums.PlatformSubscriptionStatus;
import com.planno.dash_api.enums.TenantBillingMode;
import com.planno.dash_api.repository.PlatformSubscriptionRepository;
import com.planno.dash_api.repository.TenantRepository;
import com.planno.dash_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

@Configuration
@Profile("h2")
@RequiredArgsConstructor
public class H2SeedConfig {

    private static final String TENANT_SLUG = "planno-local";
    private static final String USER_EMAIL = "teste@plannotasks.local";

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedH2Database() {
        return args -> {
            Tenant tenant = tenantRepository.findAll()
                    .stream()
                    .filter(item -> TENANT_SLUG.equalsIgnoreCase(item.getSlug()))
                    .findFirst()
                    .orElseGet(this::createTenant);

            if (!userRepository.existsByEmail(USER_EMAIL)) {
                createUser(tenant);
            }

            platformSubscriptionRepository.findByTenantId(tenant.getId())
                    .orElseGet(() -> createSubscription(tenant));
        };
    }

    private Tenant createTenant() {
        Tenant tenant = new Tenant();
        tenant.setName("Planno Tasks Local");
        tenant.setSlug(TENANT_SLUG);
        tenant.setCnpj("00000000000000");
        tenant.setActive(true);
        tenant.setBillingMode(TenantBillingMode.COMPLIMENTARY);
        return tenantRepository.save(tenant);
    }

    private void createUser(Tenant tenant) {
        User user = new User();
        user.setName("Usuario de Teste");
        user.setEmail(USER_EMAIL);
        user.setCpf("00000000000");
        user.setPassword(passwordEncoder.encode("planno123"));
        user.setTenant(tenant);
        userRepository.save(user);
    }

    private PlatformSubscription createSubscription(Tenant tenant) {
        PlatformSubscription subscription = new PlatformSubscription();
        subscription.setTenant(tenant);
        subscription.setPlanCode("planno-tasks-local");
        subscription.setPlanName("Planno Tasks Local");
        subscription.setAmount(BigDecimal.ZERO);
        subscription.setCurrencyId("BRL");
        subscription.setPayerEmail(USER_EMAIL);
        subscription.setStatus(PlatformSubscriptionStatus.ACTIVE);
        subscription.setExternalReference("h2-local-" + tenant.getId());
        return platformSubscriptionRepository.save(subscription);
    }
}
