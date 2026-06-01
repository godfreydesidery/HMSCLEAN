package com.otapp.hmis.engine.patient.application.event;

/**
 * Published by {@code PatientService.changeType} when a patient's type moves
 * <em>away</em> from OUTSIDER (a walk-in being converted into a registered
 * OUTPATIENT / INPATIENT). Their pending walk-in work is now orphaned, so
 * downstream listeners sweep it (legacy {@code change_type} cleanup):
 *
 * <ul>
 *   <li>encounter cancels the patient's open OUTSIDER orders + prescriptions;</li>
 *   <li>billing discards the patient's draft OUTSIDER invoice.</li>
 * </ul>
 *
 * Keeps the patient module free of any encounter / billing dependency — the
 * sweep is inverted through this event, like {@link PatientRegisteredEvent}.
 */
public record PatientLeftOutsiderEvent(String patientUid) {}
