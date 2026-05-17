/**
 * Patient registration, demographics, identifiers, and visits.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Patient",
        allowedDependencies = {"common", "common.*", "iam", "masterdata"}
)
package com.otapp.hmis.engine.patient;
