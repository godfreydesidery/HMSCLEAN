package com.otapp.hmis.engine.patient.domain;

/**
 * Routing classification for a patient. Drives whether clinical activity
 * has to flow through a clinic consultation, or whether services can be
 * raised directly against the patient.
 *
 * <ul>
 *   <li>{@code OUTPATIENT} — the standard pathway: register → consultation
 *       at a clinic → orders / prescriptions / admission raised inside the
 *       consultation.</li>
 *   <li>{@code OUTSIDER} — walk-in. No consultation is created. Labs,
 *       radiology, procedures, and pharmacy sales can be raised directly
 *       against the patient. Used for one-off lab requests, OTC retail,
 *       occupational health screens, etc.</li>
 * </ul>
 *
 * <p>A patient's type can be converted (e.g. an outsider becomes an
 * outpatient when they need a doctor; an outpatient becomes an outsider for
 * a one-off retail request). Conversion does not retroactively change past
 * encounters — only future activity follows the new routing rules.
 */
public enum PatientType {
    OUTPATIENT,
    OUTSIDER
}
