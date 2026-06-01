package com.otapp.hmis.engine.encounter.discharge.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Site-level configuration for the closure (discharge / referral / death)
 * workflow.
 *
 * @param allowSelfApproval when {@code true}, the author of a closure plan may
 *        also approve it. The default ({@code false}) enforces a separate
 *        approver (the legacy four-eyes control). Solo-clinician sites can flip
 *        this on (env {@code HMIS_CLOSURE_ALLOW_SELF_APPROVAL=true}) so a single
 *        doctor is not blocked from closing their own admissions/consultations.
 */
@ConfigurationProperties(prefix = "hmis.closure")
public record ClosureProperties(boolean allowSelfApproval) {
}
