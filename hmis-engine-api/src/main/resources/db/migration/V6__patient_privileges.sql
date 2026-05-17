-- ============================================================================
-- Add finer-grained patient privileges. The existing PATIENT_ACCESS gate stays
-- in place (controllers use it today) and gets granted alongside the new
-- ones so existing roles continue to work.
-- ============================================================================

INSERT INTO iam_privilege (uid, name, description,
                           created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000PT1', 'PATIENT_READ',   'View patient records',         NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PT2', 'PATIENT_CREATE', 'Register new patients',        NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PT3', 'PATIENT_UPDATE', 'Update patient demographics',  NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PT4', 'PATIENT_DELETE', 'Deactivate / delete patients', NOW(), NOW(), 'system', 'system', 0);

-- Grant the new privileges to ROOT and any role that already had PATIENT_ACCESS.
INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT r.id, p.id
FROM iam_role r
CROSS JOIN iam_privilege p
WHERE p.name IN ('PATIENT_READ','PATIENT_CREATE','PATIENT_UPDATE','PATIENT_DELETE')
  AND (
        r.name = 'ROOT'
        OR EXISTS (
            SELECT 1
            FROM iam_role_privilege existing
            JOIN iam_privilege ep ON ep.id = existing.privilege_id
            WHERE existing.role_id = r.id
              AND ep.name = 'PATIENT_ACCESS'
        )
      );
