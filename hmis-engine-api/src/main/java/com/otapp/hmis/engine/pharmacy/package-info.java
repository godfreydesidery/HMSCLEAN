/**
 * Pharmacy operations: stock balances, stock movements, and dispensing
 * prescriptions raised in the encounter module.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Pharmacy & Inventory",
        allowedDependencies = {"common", "iam", "masterdata", "patient", "encounter"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.pharmacy;
