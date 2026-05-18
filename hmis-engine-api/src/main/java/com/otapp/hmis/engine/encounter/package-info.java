/**
 * Clinical encounters: consultations, admissions, ward transfers, discharge,
 * deceased records, referrals, and nursing care.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Encounter",
        allowedDependencies = {"common", "iam", "masterdata", "patient"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.encounter;
