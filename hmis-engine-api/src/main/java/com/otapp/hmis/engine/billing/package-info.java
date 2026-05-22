/**
 * Billing, invoicing, payments, credit notes, and insurance claim submission.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Billing",
        allowedDependencies = {"common", "iam", "masterdata", "patient", "encounter"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.billing;
