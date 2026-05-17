package com.otapp.hmis.engine.common.tenant;

/**
 * Tenant identifier value object.
 *
 * <p>Today every deployment is single-tenant and resolves to {@link #DEFAULT}.
 * When multi-tenancy is introduced, this becomes the carrier passed through
 * {@link TenantContext} and persisted on multi-tenant entities — no domain
 * code needs to change shape.
 */
public record TenantId(String value) {

    public static final TenantId DEFAULT = new TenantId("default");

    public TenantId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("TenantId must not be blank");
        }
    }
}
