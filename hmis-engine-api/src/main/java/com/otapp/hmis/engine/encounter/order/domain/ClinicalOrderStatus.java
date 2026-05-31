package com.otapp.hmis.engine.encounter.order.domain;

/**
 * Clinical-order lifecycle, restored to the legacy per-service gates
 * (PROCESS_MISMATCHES.md M14/M16):
 *
 * <pre>
 *   LAB_TEST / RADIOLOGY:  REQUESTED ─► ACCEPTED ─► IN_PROGRESS ─► COMPLETED
 *                            ▲   │ ▲                              /
 *                    (re-accept) │ └─(hold: bounce to REQUESTED)─┘
 *                            └─ REJECTED ◄─(reject w/ reason)
 *   PROCEDURE:             REQUESTED ─► APPROVED ─► IN_PROGRESS ─► COMPLETED
 *                                  \                              /
 *                                   └────────► CANCELLED ◄───────┘
 * </pre>
 *
 * ACCEPTED = lab specimen collected / radiology study scheduled and accepted.
 * APPROVED = procedure signed off by the surgeon / anaesthetist. A procedure
 * cannot be worked without APPROVED; a lab/radiology order cannot be worked
 * without ACCEPTED. Completion always passes through IN_PROGRESS — the
 * REQUESTED → COMPLETED shortcut is no longer allowed.
 *
 * <p>REJECTED (lab/radiology only) is a recoverable bounce-back: a technician
 * rejects a specimen/study with a reason; the ordering side sees it and the
 * order can be accepted again (which clears the rejection). A "hold" is the
 * legacy pause — it returns an ACCEPTED order to REQUESTED and stamps who held
 * it (there is no distinct HELD state, matching legacy Zana-HMIS).
 */
public enum ClinicalOrderStatus {
    REQUESTED,
    ACCEPTED,
    APPROVED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
    REJECTED
}
