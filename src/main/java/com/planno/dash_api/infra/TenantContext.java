package com.planno.dash_api.infra;

/**
 * Armazena o ID do Tenant da requisiÃ§Ã£o atual de forma isolada por Thread.
 */
public class TenantContext {

    // ThreadLocal que guarda o ID do Tenant (Long)
    private static final ThreadLocal<Long> currentTenant = new ThreadLocal<>();

    /**
     * Define o Tenant ID para a thread atual.
     * Chamado pelo SecurityFilter apÃ³s validar o Token JWT.
     */
    public static void setTenantId(Long tenantId) {
        currentTenant.set(tenantId);
    }

    /**
     * Recupera o Tenant ID da thread atual.
     * Usado nos Services para filtrar buscas e salvar registros.
     */
    public static Long getTenantId() {
        return currentTenant.get();
    }

    /**
     * Limpa o valor do ThreadLocal.
     * CRUCIAL: Deve ser chamado no bloco 'finally' do filtro para evitar
     * que o ID permaneÃ§a em threads recicladas pelo servidor.
     */
    public static void clear() {
        currentTenant.remove();
    }
}
