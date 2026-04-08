package com.planno.dash_api.service;

import com.planno.dash_api.entity.User;
import com.planno.dash_api.infra.exception.ForbiddenException;
import com.planno.dash_api.infra.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CurrentUserService {

    @Value("${app.platform-billing.admin-tenant-slug:}")
    private String adminTenantSlug;

    public User getCurrentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (!(principal instanceof User user)) {
            throw new UnauthorizedException("Usuario nao autenticado.");
        }

        return user;
    }

    public Long getCurrentTenantId() {
        return getCurrentUser().getTenant().getId();
    }

    public boolean isBillingAdminTenant() {
        User user = getCurrentUser();
        return StringUtils.hasText(adminTenantSlug)
                && adminTenantSlug.equalsIgnoreCase(user.getTenant().getSlug());
    }

    public void ensureBillingAdminTenant() {
        if (!isBillingAdminTenant()) {
            throw new ForbiddenException("Esta area de assinaturas e exclusiva da equipe interna do sistema.");
        }
    }
}
