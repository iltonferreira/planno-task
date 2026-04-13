package com.planno.dash_api.infra;

public record ErrorMessage(
        int status,
        String message
) {
}

