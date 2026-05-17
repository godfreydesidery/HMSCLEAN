/**
 * Clinical orders and results: lab tests, radiology, procedures, and
 * prescriptions, with their attachments and result lifecycle.
 *
 * <p>Module not yet implemented.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Orders & Results",
        allowedDependencies = {"common", "common.*", "iam", "masterdata", "patient", "encounter"}
)
package com.otapp.hmis.engine.orders;
