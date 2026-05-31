package com.otapp.hmis.engine.common.spi;

/**
 * Loose-coupling SPI (shared kernel) that lets the patient module ask "does this
 * patient currently have an active encounter?" without depending on the encounter
 * module. The encounter module (which already depends on patient) provides the
 * implementing bean; the patient module depends only on this interface in common.
 *
 * <p>Mirrors the established billing → encounter / encounter → patient one-way
 * dependency rule: patient must NOT import encounter, so the guard is inverted
 * through this port.
 */
public interface EncounterActivityPort {

    /**
     * True if the patient has an ongoing encounter — an active consultation
     * (BOOKED / IN_PROGRESS / TRANSFERRED) or an active (ADMITTED) admission.
     * Used to block a patient type / payment-type change while an encounter is
     * in progress (legacy change_type / change_payment_type gates).
     */
    boolean hasActiveEncounter(String patientUid);
}
