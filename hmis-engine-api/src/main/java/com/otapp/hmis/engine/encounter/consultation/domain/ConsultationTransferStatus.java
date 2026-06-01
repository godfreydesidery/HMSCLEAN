package com.otapp.hmis.engine.encounter.consultation.domain;

/**
 * Lifecycle of a consultation transfer request (legacy Zana-HMIS two-phase
 * clinic-to-clinic hand-off).
 *
 * <pre>
 *   PENDING ──► COMPLETED  (reception accepted; a fresh consultation was booked)
 *        └────► CANCELLED  (the initiating doctor reverted the request)
 * </pre>
 *
 * <p>A transfer starts PENDING when the treating doctor hands the patient off to
 * another clinic (the source consultation flips to TRANSFERRED). Reception later
 * picks it up — choosing the receiving clinician — which COMPLETES it; or the
 * doctor reverts it, which CANCELs it and restores the source to IN_PROGRESS.
 */
public enum ConsultationTransferStatus {
    PENDING,
    COMPLETED,
    CANCELLED
}
