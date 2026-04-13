package com.planno.dash_api.dto.response;

public record TenantResponseDTO(
    Long id,
    String name,
    String slug,
    boolean active,
    String billingMode
){}
