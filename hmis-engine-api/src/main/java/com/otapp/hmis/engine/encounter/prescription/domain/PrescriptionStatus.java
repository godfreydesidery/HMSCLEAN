package com.otapp.hmis.engine.encounter.prescription.domain;

/**
 * Full prescription lifecycle, matching the proven Zana-HMIS process
 * (PROCESS.md §8.1, §15). Each stage represents a distinct pharmacy
 * touch-point, not a UI fiction:
 *
 * <pre>
 *   PENDING ──► ACCEPTED ──► HELD ──► VERIFIED ──► APPROVED ──► SOLD
 *      │            │         │         │           │
 *      │            └─────────┴─────────┴───────────┴──► REJECTED
 *      └──► CANCELLED
 * </pre>
 *
 * <ul>
 *   <li>{@code PENDING}   — created by the doctor / pharmacist (initial state).</li>
 *   <li>{@code ACCEPTED}  — pharmacist has picked the item up off the queue.</li>
 *   <li>{@code HELD}      — paused: awaiting payment (cash patients) or
 *       awaiting stock arrival. Resumes by re-VERIFY.</li>
 *   <li>{@code VERIFIED}  — clinical / stock quality check passed.</li>
 *   <li>{@code APPROVED}  — final approval, ready for dispense.</li>
 *   <li>{@code SOLD}      — dispensed to the patient; stock decremented.</li>
 *   <li>{@code REJECTED}  — pharmacist refused (wrong dose, missing payment,
 *       interaction flagged). Terminal.</li>
 *   <li>{@code CANCELLED} — withdrawn by prescriber before pharmacy worked
 *       it. Terminal.</li>
 * </ul>
 */
public enum PrescriptionStatus {
    PENDING,
    ACCEPTED,
    HELD,
    VERIFIED,
    APPROVED,
    SOLD,
    REJECTED,
    CANCELLED
}
