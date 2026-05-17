/**
 * Clinical encounters: consultations, admissions, ward transfers, discharge,
 * deceased records, referrals, and nursing care.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Encounter",
        allowedDependencies = {"common", "common.*", "iam", "masterdata", "patient"}
)
package com.otapp.hmis.engine.encounter;
