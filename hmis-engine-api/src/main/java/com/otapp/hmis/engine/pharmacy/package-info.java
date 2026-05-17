/**
 * Pharmacy operations: stock balances, stock movements, and dispensing
 * prescriptions raised in the encounter module.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Pharmacy & Inventory",
        allowedDependencies = {"common", "common.*", "iam", "masterdata", "encounter"}
)
package com.otapp.hmis.engine.pharmacy;
