package com.otapp.hmis.engine.transfer.common.domain;

/**
 * Lifecycle of a Receive Note (RN) — shared across pharmacy ↔ store and
 * pharmacy ↔ pharmacy transfers. Today RNs are confirmed in one shot —
 * the receiver signs for what arrived and the document closes — so the
 * state machine is intentionally thin. Kept as a distinct enum from
 * {@link TransferDocStatus} so a future PARTIALLY_RECEIVED or DISPUTED
 * state can land without churning the RO/TO lifecycle.
 */
public enum ReceiveNoteStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}
