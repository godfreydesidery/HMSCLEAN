/**
 * Master data: clinics, wards, pharmacies, stores, catalogs (lab/radiology/
 * procedure types, medicines, dressings, consumables), insurance providers
 * and plans, and the pricing matrix.
 */
@org.springframework.modulith.ApplicationModule(
        displayName = "Master Data",
        allowedDependencies = {"common", "iam"},
        type = org.springframework.modulith.ApplicationModule.Type.OPEN
)
package com.otapp.hmis.engine.masterdata;
