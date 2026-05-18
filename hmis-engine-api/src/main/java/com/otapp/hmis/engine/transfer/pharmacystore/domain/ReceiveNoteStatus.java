package com.otapp.hmis.engine.transfer.pharmacystore.domain;

/**
 * Lifecycle of a Receive Note. Today RNs are confirmed in one shot — the
 * pharmacy signs for what arrived and the document closes — so the state
 * machine is intentionally thin. Kept as a distinct enum from
 * {@link TransferDocStatus} so a future "PARTIALLY_RECEIVED" or DISPUTED
 * state can land without churning the RO/TO lifecycle.
 */
public enum ReceiveNoteStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}
