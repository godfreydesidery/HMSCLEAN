/**
 * Central store domain: stock balances, batches, and movements for a
 * hospital-wide store. Pharmacies pull from here via the transfer module.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Store",
        allowedDependencies = {"common", "iam", "masterdata"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.store;
