/**
 * Pharmacy ↔ store transfer chains: requisition (RO), issue (TO), receipt
 * (RN), and pharmacy-to-store returns. Each chain orchestrates stock
 * movements across the pharmacy and store modules.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Transfers",
        allowedDependencies = {"common", "iam", "masterdata", "pharmacy", "store"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.transfer;
