package com.otapp.hmis.engine.masterdata.bed.domain;

/**
 * Current physical state of one ward bed.
 *
 * <pre>
 *   FREE        ──► OCCUPIED ──► FREE
 *      \                   \─►  OUT_OF_SERVICE
 *       └──► OUT_OF_SERVICE ──► FREE
 *
 *   RESERVED is set when a bed is held for an admission that hasn't
 *   physically claimed it yet (rare workflow).
 * </pre>
 */
public enum BedStatus {
    FREE,
    OCCUPIED,
    RESERVED,
    OUT_OF_SERVICE
}
