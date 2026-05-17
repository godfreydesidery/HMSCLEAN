/**
 * Cross-module read models, dashboards, and report generation.
 *
 * <p>Module not yet implemented.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Reporting",
        allowedDependencies = {"common", "common.*", "iam", "masterdata", "patient", "encounter", "orders", "pharmacy", "procurement", "billing", "hr"}
)
package com.otapp.hmis.engine.reporting;
