/**
 * Patient registration, demographics, identifiers, and visits.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Patient",
        allowedDependencies = {"common", "iam", "masterdata"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.patient;
