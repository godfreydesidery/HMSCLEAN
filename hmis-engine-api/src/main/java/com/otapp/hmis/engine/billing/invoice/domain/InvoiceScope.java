package com.otapp.hmis.engine.billing.invoice.domain;

/**
 * What this invoice was raised for. Registration invoices have the same
 * (consultationUid=null, admissionUid=null) shape as outsider walk-in
 * invoices, so an explicit discriminator is required to keep the two from
 * colliding in queries like {@code findDraftOutsiderForPatient}.
 */
public enum InvoiceScope {
    CONSULTATION,
    ADMISSION,
    OUTSIDER,
    REGISTRATION
}
