/**
 * Pharmacy operations: dispensing, stock cards, batch tracking, and the
 * inter-store / inter-pharmacy transfer matrix (RO / TO / RN documents).
 *
 * <p>Module not yet implemented.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Pharmacy & Inventory",
        allowedDependencies = {"common", "common.*", "iam", "masterdata", "orders"}
)
package com.otapp.hmis.engine.pharmacy;
