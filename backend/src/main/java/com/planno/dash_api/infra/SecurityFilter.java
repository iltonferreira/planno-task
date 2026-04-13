package com.planno.dash_api.infra;

import com.planno.dash_api.enums.TenantBillingMode;
import com.planno.dash_api.repository.PlatformSubscriptionRepository;
import com.planno.dash_api.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    @Value("${app.platform-billing.admin-tenant-slug:}")
    private String adminTenantSlug;

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final PlatformSubscriptionRepository platformSubscriptionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        var token = this.recoverToken(request);

        if (token != null) {
            var email = tokenService.validateToken(token);
            if (email != null && !email.isBlank()) {
                var user = userRepository.findByEmail(email).orElse(null);
                if (user == null) {
                    unauthorized(response);
                    return;
                }

                TenantContext.setTenantId(user.getTenant().getId());

                if (!isBillingAdminTenant(user.getTenant().getSlug())
                        && (!user.getTenant().isActive() || isBlockedByPlatformBilling(user.getTenant().getId(), user.getTenant().getBillingMode(), request.getServletPath()))) {
                    paymentRequired(response);
                    return;
                }

                var authentication = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                unauthorized(response);
                return;
            }
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
            TenantContext.clear();
        }
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        if (!authHeader.toLowerCase(Locale.ROOT).startsWith("bearer ")) return null;
        String token = authHeader.substring(7).trim();
        return token.isBlank() ? null : token;
    }

    private void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":401,\"message\":\"Token invalido ou expirado.\"}");
    }

    private void paymentRequired(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_PAYMENT_REQUIRED);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\":402,\"message\":\"O workspace esta com o plano inativo. Acesse Meu plano para regularizar.\"}");
    }

    private boolean isBlockedByPlatformBilling(Long tenantId, TenantBillingMode billingMode, String path) {
        if (path == null || path.startsWith("/api/platform-billing") || path.startsWith("/api/users/me")) {
            return false;
        }

        if (billingMode == TenantBillingMode.COMPLIMENTARY) {
            return false;
        }

        return platformSubscriptionRepository.findByTenantId(tenantId)
                .map(subscription -> !"ACTIVE".equals(subscription.getStatus().name()))
                .orElse(true);
    }

    private boolean isBillingAdminTenant(String tenantSlug) {
        return StringUtils.hasText(adminTenantSlug) && adminTenantSlug.equalsIgnoreCase(tenantSlug);
    }
}
