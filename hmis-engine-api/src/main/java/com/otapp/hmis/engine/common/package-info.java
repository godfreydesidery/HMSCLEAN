/**
 * Shared kernel for the HMIS engine: cross-cutting types used by all modules.
 *
 * <p>Contains the tenant context, base entity, error model, pagination types,
 * and other building blocks that must not depend on any business module.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Common (shared kernel)"
)
package com.otapp.hmis.engine.common;
