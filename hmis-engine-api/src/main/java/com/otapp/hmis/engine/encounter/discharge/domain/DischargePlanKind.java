package com.otapp.hmis.engine.encounter.discharge.domain;

import com.otapp.hmis.engine.encounter.admission.domain.AdmissionStatus;

/**
 * The three terminal closure paths a discharge plan can drive an
 * admission to when APPROVED. Each kind maps to an {@link AdmissionStatus}
 * (DISCHARGE → DISCHARGED, DECEASED → DECEASED, REFERRAL → TRANSFERRED)
 * and surfaces a different set of required fields:
 *
 * <ul>
 *   <li>{@code DISCHARGE} — standard discharge home; clinical narrative + recommendations.</li>
 *   <li>{@code DECEASED} — death-in-hospital; requires time + cause of death.</li>
 *   <li>{@code REFERRAL} — transferred out to another facility; requires
 *       receiving facility + referral reason.</li>
 * </ul>
 */
public enum DischargePlanKind {
    DISCHARGE,
    DECEASED,
    REFERRAL
}
