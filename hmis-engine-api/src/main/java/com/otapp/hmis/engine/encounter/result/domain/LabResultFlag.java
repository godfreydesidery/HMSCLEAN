package com.otapp.hmis.engine.encounter.result.domain;

/**
 * Abnormal-flag computed for a lab result line by comparing the measured value
 * against the snapshotted reference range.
 *
 * <ul>
 *   <li>{@code NONE} — no usable reference range (value recorded, not flagged).</li>
 *   <li>{@code NORMAL} — within the reference range.</li>
 *   <li>{@code LOW}/{@code HIGH} — outside the reference range.</li>
 *   <li>{@code CRITICAL_LOW}/{@code CRITICAL_HIGH} — outside the panic bounds.</li>
 *   <li>{@code ABNORMAL} — a TEXT result that did not match the expected normal text.</li>
 * </ul>
 */
public enum LabResultFlag {
    NONE,
    NORMAL,
    LOW,
    HIGH,
    CRITICAL_LOW,
    CRITICAL_HIGH,
    ABNORMAL
}
