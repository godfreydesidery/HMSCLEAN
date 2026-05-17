/**
 * Billing, invoicing, payments, credit notes, and insurance claim submission.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Billing",
        allowedDependencies = {"common", "common.*", "iam", "masterdata", "patient", "encounter"}
)
package com.otapp.hmis.engine.billing;
