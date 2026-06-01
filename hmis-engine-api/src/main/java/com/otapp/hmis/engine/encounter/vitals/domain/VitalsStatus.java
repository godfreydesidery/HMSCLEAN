package com.otapp.hmis.engine.encounter.vitals.domain;

/**
 * Lifecycle of a {@link PatientVitals} row — a faithful port of the legacy
 * Zana-HMIS {@code PatientVital} two-actor capture flow (OPC-3).
 *
 * <pre>
 *   EMPTY ──► PENDING ──► SUBMITTED ──► ARCHIVED
 * </pre>
 *
 * <ul>
 *   <li><b>EMPTY</b> — the row is materialised the moment the encounter needs
 *       vitals (legacy: when the doctor opens the consultation). The EMPTY row
 *       <em>is</em> the implicit request; there is no separate request task.</li>
 *   <li><b>PENDING</b> — the nurse has filled (saved) the readings; still
 *       editable until submitted.</li>
 *   <li><b>SUBMITTED</b> — the nurse has submitted the set; locked, cannot be
 *       re-edited or re-submitted.</li>
 *   <li><b>ARCHIVED</b> — the doctor has consumed the submitted set into the
 *       clinical note (legacy copied the values into a GeneralExamination and
 *       flagged the vital archived). Terminal.</li>
 * </ul>
 *
 * <p>Historical inline-recorded vitals (captured before this lifecycle existed)
 * are migrated to {@code SUBMITTED} so they remain valid and a doctor can still
 * consume them.
 */
public enum VitalsStatus {
    EMPTY,
    PENDING,
    SUBMITTED,
    ARCHIVED
}
