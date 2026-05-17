/**
 * Master data: clinics, wards, pharmacies, stores, catalogs (lab/radiology/
 * procedure types, medicines, dressings, consumables), insurance providers
 * and plans, and the pricing matrix.
 *
 * <p>Module not yet implemented.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Master Data",
        allowedDependencies = {"common", "common.*", "iam"}
)
package com.otapp.hmis.engine.masterdata;
