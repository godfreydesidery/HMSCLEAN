package com.otapp.hmis.engine.encounter.progressnote.domain;

/**
 * The kind of progress note recorded during an admission. The kind
 * drives sorting/filtering in the UI and affects which roles tend
 * to author them; it doesn't change persistence shape.
 */
public enum ProgressNoteKind {
    DOCTOR,
    NURSING,
    OBSERVATION,
    HANDOVER
}
