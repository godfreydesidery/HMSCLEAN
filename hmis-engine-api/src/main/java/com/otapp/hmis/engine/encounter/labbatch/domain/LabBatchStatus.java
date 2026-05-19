package com.otapp.hmis.engine.encounter.labbatch.domain;

/**
 * Workflow status of a lab-batch — purely organisational. The batch
 * doesn't change individual order statuses; it groups N orders of the
 * same lab test type so the tech can process them together.
 *
 * <pre>
 *   OPEN ──► PROCESSING ──► COMPLETED
 *        \─► CANCELLED
 * </pre>
 */
public enum LabBatchStatus {
    OPEN,
    PROCESSING,
    COMPLETED,
    CANCELLED
}
