package com.planno.dash_api.dto.mapper;

import com.planno.dash_api.dto.request.TenantRequestDTO;
import com.planno.dash_api.dto.response.TenantResponseDTO;
import com.planno.dash_api.entity.Tenant;
import com.planno.dash_api.enums.TenantBillingMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TenantMapper {

    public Tenant toEntity(TenantRequestDTO dto) {
        Tenant tenant = new Tenant();
        tenant.setName(dto.name());
        tenant.setSlug(dto.slug());
        tenant.setCnpj(dto.cnpj());
        tenant.setActive(true);

        if (StringUtils.hasText(dto.billingMode())) {
            tenant.setBillingMode(TenantBillingMode.valueOf(dto.billingMode()));
        }

        return tenant;
    }

    public TenantResponseDTO toResponseDTO(Tenant tenant) {
        return new TenantResponseDTO(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.isActive(),
                tenant.getBillingMode().name()
        );
    }
}
