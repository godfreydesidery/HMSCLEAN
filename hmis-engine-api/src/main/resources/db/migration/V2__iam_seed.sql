-- ============================================================================
-- Seed baseline IAM data: privileges, roles, and role-privilege grants.
--
-- UIDs are hardcoded 26-char ULIDs (Crockford base32) so the seeded rows
-- have stable identifiers across deployments. The root user is created at
-- application startup (see IamBootstrap.java) where its ULID is generated
-- by the JPA layer using ulid-creator.
-- ============================================================================

INSERT INTO iam_privilege (uid, name, description, created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000PV1', 'USER_READ',          'View users',                       NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PV2', 'USER_CREATE',        'Create users',                     NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PV3', 'USER_UPDATE',        'Update users (incl. role grants)', NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PV4', 'USER_DELETE',        'Delete users',                     NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PV5', 'ROLE_READ',          'View roles and privileges',        NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PV6', 'ROLE_CREATE',        'Create roles',                     NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PV7', 'ROLE_UPDATE',        'Update roles and their privileges',NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PV8', 'ROLE_DELETE',        'Delete roles',                     NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PV9', 'PATIENT_ACCESS',     'Access the patient module',        NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVA', 'ENCOUNTER_ACCESS',   'Access the encounter module',      NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVB', 'ORDERS_ACCESS',      'Access the orders module',         NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVC', 'PHARMACY_ACCESS',    'Access the pharmacy module',       NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVD', 'PROCUREMENT_ACCESS', 'Access the procurement module',    NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVE', 'BILLING_ACCESS',     'Access the billing module',        NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVF', 'HR_ACCESS',          'Access the HR module',             NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVG', 'REPORTING_ACCESS',   'Access the reporting module',      NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVH', 'MASTERDATA_MANAGE',  'Manage master data catalogs',      NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVJ', 'STORE_ACCESS',       'Access the central store module',  NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVK', 'PROCUREMENT_VERIFY', 'Verify purchase orders / GRNs (procurement manager)', NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000PVL', 'PROCUREMENT_APPROVE','Approve purchase orders / GRNs (director)',           NOW(), NOW(), 'system', 'system', 0);

INSERT INTO iam_role (uid, name, description, created_at, updated_at, created_by, updated_by, version) VALUES
    ('01J5KQRPCD0000000000000RB1', 'ROOT',         'Super administrator (all privileges)',          NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RB2', 'ADMIN',        'System administrator',                          NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RB3', 'RECEPTION',    'Patient registration and reception',            NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RB4', 'CLINICIAN',    'Doctor / clinical practitioner',                NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RB5', 'NURSE',        'Nursing staff',                                 NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RB6', 'PHARMACIST',   'Pharmacy staff',                                NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RB7', 'LABORATORIST', 'Lab technician',                                NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RB8', 'RADIOGRAPHER', 'Radiology technician',                          NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RB9', 'CASHIER',      'Cashier / billing clerk',                       NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RBA', 'ACCOUNTANT',   'Accounting',                                    NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RBB', 'HR',           'Human resources',                               NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RBC', 'PROCUREMENT',  'Procurement and supplier management',           NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RBD', 'STORE_PERSON', 'Inventory store keeper',                        NOW(), NOW(), 'system', 'system', 0),
    ('01J5KQRPCD0000000000000RBE', 'MANAGEMENT',   'Management dashboards and reports (read-only)', NOW(), NOW(), 'system', 'system', 0);

-- Grant every privilege to ROOT
INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'ROOT'), p.id
FROM iam_privilege p;

-- Grant IAM management privileges to ADMIN
INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'ADMIN'), p.id
FROM iam_privilege p
WHERE p.name IN ('USER_READ','USER_CREATE','USER_UPDATE','USER_DELETE',
                 'ROLE_READ','ROLE_CREATE','ROLE_UPDATE','ROLE_DELETE',
                 'MASTERDATA_MANAGE');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'RECEPTION'), p.id
FROM iam_privilege p
WHERE p.name IN ('PATIENT_ACCESS','BILLING_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'CLINICIAN'), p.id
FROM iam_privilege p
WHERE p.name IN ('PATIENT_ACCESS','ENCOUNTER_ACCESS','ORDERS_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'NURSE'), p.id
FROM iam_privilege p
WHERE p.name IN ('PATIENT_ACCESS','ENCOUNTER_ACCESS','ORDERS_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'PHARMACIST'), p.id
FROM iam_privilege p
WHERE p.name IN ('PHARMACY_ACCESS','ORDERS_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'LABORATORIST'), p.id
FROM iam_privilege p
WHERE p.name IN ('ORDERS_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'RADIOGRAPHER'), p.id
FROM iam_privilege p
WHERE p.name IN ('ORDERS_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'CASHIER'), p.id
FROM iam_privilege p
WHERE p.name IN ('BILLING_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'ACCOUNTANT'), p.id
FROM iam_privilege p
WHERE p.name IN ('BILLING_ACCESS','REPORTING_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'HR'), p.id
FROM iam_privilege p
WHERE p.name IN ('HR_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'PROCUREMENT'), p.id
FROM iam_privilege p
WHERE p.name IN ('PROCUREMENT_ACCESS','STORE_ACCESS','PROCUREMENT_VERIFY');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'STORE_PERSON'), p.id
FROM iam_privilege p
WHERE p.name IN ('STORE_ACCESS','PROCUREMENT_ACCESS');

INSERT INTO iam_role_privilege (role_id, privilege_id)
SELECT (SELECT id FROM iam_role WHERE name = 'MANAGEMENT'), p.id
FROM iam_privilege p
WHERE p.name IN ('REPORTING_ACCESS','BILLING_ACCESS','PROCUREMENT_APPROVE');
