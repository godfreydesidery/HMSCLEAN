package com.otapp.hmis.engine.encounter.prescription.application.event;

/**
 * Published when a consultation-bound prescription is raised. Billing listens
 * after-commit and bills the medicine onto the consultation invoice up front so
 * a CASH patient's medicine can be paid before it is dispensed
 * (PROCESS_MISMATCHES.md M13). Outsider retail prescriptions are billed via the
 * on-demand outsider invoice and do not publish this event.
 */
public record PrescriptionRaisedEvent(String prescriptionUid) {}
