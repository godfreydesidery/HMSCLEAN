/**
 * Billing, invoicing, payments, credit notes, and insurance claim submission.
 *
 * <p>Module not yet implemented.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Billing",
        allowedDependencies = {"common", "common.*", "iam", "masterdata", "patient", "encounter", "orders", "pharmacy"}
)
package com.otapp.hmis.engine.billing;
