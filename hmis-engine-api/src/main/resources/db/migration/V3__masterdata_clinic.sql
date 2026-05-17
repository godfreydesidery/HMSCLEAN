-- ============================================================================
-- Master data: clinics (specialty / departmental units that host consultations)
-- ============================================================================

CREATE TABLE md_clinic (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32) NOT NULL,
    name        VARCHAR(120) NOT NULL,
    type        VARCHAR(32) NOT NULL,
    description VARCHAR(500),
    location    VARCHAR(80),
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_md_clinic_code UNIQUE (code)
);

CREATE INDEX idx_md_clinic_active ON md_clinic(active);
CREATE INDEX idx_md_clinic_type   ON md_clinic(type);

-- Sample clinics seeded for first-run convenience. Operators can deactivate
-- or delete these from the admin UI.
INSERT INTO md_clinic (code, name, type, description, location, active,
                       created_at, updated_at, created_by, updated_by, version)
VALUES
    ('OPD',  'General Outpatient',  'OUTPATIENT', 'Walk-in consultations',    'Block A',  TRUE,
     NOW(), NOW(), 'system', 'system', 0),
    ('PED',  'Pediatrics',          'SPECIALTY',  'Children up to 18 years',  'Block B',  TRUE,
     NOW(), NOW(), 'system', 'system', 0),
    ('ER',   'Emergency',           'EMERGENCY',  '24/7 emergency unit',      'Ground floor', TRUE,
     NOW(), NOW(), 'system', 'system', 0);
