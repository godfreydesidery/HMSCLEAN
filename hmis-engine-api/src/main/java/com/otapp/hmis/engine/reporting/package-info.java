/**
 * Cross-module read models, dashboards, and report generation.
 *
 * <p>Module not yet implemented.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Reporting",
        allowedDependencies = {"common", "iam", "masterdata", "patient", "encounter", "orders", "pharmacy", "procurement", "billing", "hr", "store", "transfer"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.reporting;
