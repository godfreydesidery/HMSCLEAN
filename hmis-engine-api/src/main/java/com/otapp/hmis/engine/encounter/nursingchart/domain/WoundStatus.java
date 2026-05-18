package com.otapp.hmis.engine.encounter.nursingchart.domain;

/**
 * Coarse-grained assessment of a wound at the time of dressing. Free-text
 * notes on the entry carry any additional detail (exudate, odour,
 * surrounding skin condition, etc.).
 */
public enum WoundStatus {
    CLEAN,
    HEALING,
    GRANULATING,
    SLOUGHY,
    INFECTED,
    NECROTIC,
    DEHISCED
}
