package com.otapp.hmis.engine.common.tenant;

/**
 * Per-request tenant context. Single-tenant deployments resolve to
 * {@link TenantId#DEFAULT}; in a future multi-tenant deployment, a filter
 * populates this from the JWT or a header before request handling.
 */
public final class TenantContext {

    private static final ThreadLocal<TenantId> CURRENT = ThreadLocal.withInitial(() -> TenantId.DEFAULT);

    private TenantContext() {
    }

    public static TenantId current() {
        return CURRENT.get();
    }

    public static void set(TenantId tenantId) {
        CURRENT.set(tenantId == null ? TenantId.DEFAULT : tenantId);
    }

    public static void clear() {
        CURRENT.remove();
    }
}
