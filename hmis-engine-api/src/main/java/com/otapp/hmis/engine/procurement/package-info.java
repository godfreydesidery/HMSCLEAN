/**
 * Procurement: suppliers, supplier price lists, local purchase orders, and
 * goods received notes.
 *
 * <p>Module not yet implemented.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Procurement",
        allowedDependencies = {"common", "iam", "masterdata", "pharmacy", "store", "billing"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.procurement;
