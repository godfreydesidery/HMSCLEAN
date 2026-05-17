package com.otapp.hmis.engine.pharmacy.sale.domain;

/**
 * Lifecycle of a single line on a pharmacy retail sale order — mirrors the
 * proven prescription state machine (PROCESS.md §8.1, §15) since the legacy
 * Zana-HMIS treats retail and Rx as the same workflow at the line level.
 *
 * <pre>
 *   PENDING ──► ACCEPTED ──► HELD ──► VERIFIED ──► APPROVED ──► SOLD
 *      │            │         │         │           │
 *      │            └─────────┴─────────┴───────────┴──► REJECTED
 *      └──► CANCELLED
 * </pre>
 *
 * Held separately from {@code PrescriptionStatus} (same values today) so
 * future divergence between the two flows doesn't ripple.
 */
public enum PharmacySaleLineStatus {
    PENDING,
    ACCEPTED,
    HELD,
    VERIFIED,
    APPROVED,
    SOLD,
    REJECTED,
    CANCELLED
}
