package com.otapp.hmis.engine.billing.invoice.domain;

/**
 * Per-line payer routing, copied from the legacy {@code PatientBill.status}
 * charge-time values (Zana-HMIS {@code PatientServiceImpl}). Distinct from the
 * invoice-level {@link InvoiceStatus} lifecycle.
 *
 * <ul>
 *   <li>{@link #COVERED}  — the patient's plan covers this service: insurer pays
 *       in full at the plan price, the line is settled and routed to the
 *       insurer claim. Membership number + payer plan are stamped on the line.</li>
 *   <li>{@link #VERIFIED} — an insured patient whose plan does NOT cover this
 *       service, charged on an inpatient (admission) invoice: the hospital
 *       fronts/accrues it but the insurer will not cover, so it is still owed
 *       (self-pay). Not auto-settled.</li>
 *   <li>{@link #UNPAID}   — cash, owed; settles when the invoice is paid.</li>
 * </ul>
 */
public enum LineCoverageStatus {
    COVERED,
    VERIFIED,
    UNPAID
}
