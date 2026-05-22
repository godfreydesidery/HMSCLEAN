-- ============================================================================
-- Phase 29: real bed model (PROCESS.md §17.13). Replaces the previous
-- "ward.capacity int + free-text admission.bed_label" placeholder with
-- a per-bed entity that tracks status (FREE / OCCUPIED / RESERVED /
-- OUT_OF_SERVICE) and is claimed / released by AdmissionService on
-- admit / transfer / discharge.
-- ============================================================================

CREATE TABLE md_bed (
    id                          BIGSERIAL PRIMARY KEY,
    uid                         VARCHAR(26)  NOT NULL,

    ward_uid                    VARCHAR(26)  NOT NULL,
    label                       VARCHAR(32)  NOT NULL,
    notes                       VARCHAR(500),
    status                      VARCHAR(16)  NOT NULL,
    occupied_by_admission_uid   VARCHAR(26),
    active                      BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at                  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by                  VARCHAR(80),
    updated_by                  VARCHAR(80),
    version                     BIGINT,
    CONSTRAINT uk_md_bed_uid        UNIQUE (uid),
    CONSTRAINT uk_md_bed_ward_label UNIQUE (ward_uid, label)
);

CREATE INDEX idx_md_bed_ward   ON md_bed(ward_uid);
CREATE INDEX idx_md_bed_status ON md_bed(status);

-- ----- Admission: typed bed reference alongside the free-text label --------

ALTER TABLE admission ADD COLUMN bed_uid VARCHAR(26);
CREATE INDEX idx_admission_bed ON admission(bed_uid);
