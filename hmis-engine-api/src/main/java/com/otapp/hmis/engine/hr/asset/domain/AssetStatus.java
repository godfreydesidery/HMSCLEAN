package com.otapp.hmis.engine.hr.asset.domain;

/**
 * Lifecycle states for a fixed asset on the register.
 *
 * <ul>
 *   <li>{@code ACTIVE}   — in service.</li>
 *   <li>{@code RETIRED}  — taken out of service but still in the inventory
 *                          (e.g. mothballed, awaiting disposal).</li>
 *   <li>{@code DISPOSED} — sold, scrapped, or written off; terminal.</li>
 *   <li>{@code LOST}     — gone missing; terminal.</li>
 * </ul>
 */
public enum AssetStatus {
    ACTIVE,
    RETIRED,
    DISPOSED,
    LOST
}
