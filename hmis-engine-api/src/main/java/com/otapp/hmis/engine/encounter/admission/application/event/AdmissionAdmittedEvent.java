package com.otapp.hmis.engine.encounter.admission.application.event;

/**
 * Published when a patient is admitted, so billing can seed the admission invoice
 * (ward-bed charge) at admit time — the legacy "doAdmission creates the ward-bed
 * bill" step. Billing consumes this in the allowed billing → encounter direction
 * (the encounter module never imports billing); the patient ledger / deposit gate
 * is then driven by that invoice's settlement.
 */
public record AdmissionAdmittedEvent(String admissionUid) {}
