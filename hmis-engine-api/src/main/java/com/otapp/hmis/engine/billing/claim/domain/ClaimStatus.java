package com.otapp.hmis.engine.billing.claim.domain;

/**
 * Lifecycle of an insurance claim. The legacy had no such machine (covered bills
 * settled at charge time and "submission" was a report) — this is designed fresh.
 *
 * <pre>
 *   DRAFT ──submit──▶ SUBMITTED ──settle(partial)──▶ PARTIALLY_SETTLED ──settle(rest)──▶ SETTLED
 *                         │                                  │
 *                         └────────────── reject ────────────┴──▶ REJECTED
 * </pre>
 * SETTLED and REJECTED are terminal.
 */
public enum ClaimStatus {
    DRAFT,
    SUBMITTED,
    PARTIALLY_SETTLED,
    SETTLED,
    REJECTED
}
