-- ============================================================================
-- Insurance claim ledger — per-payer aggregation of COVERED invoice lines into
-- a submittable/settleable claim.
--
-- Legacy Zana-HMIS had NO claim aggregate: covered PatientBills were appended to
-- a per-(patient, plan) PENDING PatientInvoice and "submission" was merely a
-- report; covered bills settled at charge time (balance 0). This ledger inherits
-- the per-payer grouping (here keyed off the line's stamped payer_plan_uid +
-- membership_no) and adds the submit → settle/reject lifecycle the legacy lacked.
--
-- It is a READ/AGGREGATE + lifecycle layer over the EXISTING coverage routing
-- (invoice_line.coverage_status = COVERED, payer_plan_uid, membership_no;
-- invoice.total_covered already nets the covered portion out of the patient
-- balance). Building a claim NEVER re-touches patient-balance math.
--
-- CONVENTION: cross-module references stay uid (payer_plan_uid -> masterdata,
-- provider_uid -> masterdata, patient_uid -> patient, service_uid -> masterdata);
-- INTRA-billing relationships use numeric id foreign keys with real REFERENCES.
-- ============================================================================

CREATE SEQUENCE insurance_claim_no_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE insurance_claims (
    id                    BIGSERIAL PRIMARY KEY,
    uid                   VARCHAR(26)   NOT NULL,
    claim_no              VARCHAR(32)   NOT NULL,
    payer_plan_uid        VARCHAR(26)   NOT NULL,   -- cross-module -> masterdata insurance plan
    provider_uid          VARCHAR(26)   NOT NULL,   -- cross-module -> masterdata insurance provider
    membership_no         VARCHAR(64)   NOT NULL,
    patient_uid           VARCHAR(26)   NOT NULL,   -- cross-module -> patient
    currency              VARCHAR(3)    NOT NULL,
    claimed_amount        NUMERIC(14,2) NOT NULL,
    settled_amount        NUMERIC(14,2) NOT NULL DEFAULT 0,
    status                VARCHAR(16)   NOT NULL,
    line_count            INTEGER       NOT NULL,
    submitted_at          TIMESTAMP WITH TIME ZONE,
    settled_at            TIMESTAMP WITH TIME ZONE,
    rejected_at           TIMESTAMP WITH TIME ZONE,
    rejection_reason      VARCHAR(500),
    submitted_by_username VARCHAR(64),
    settled_by_username   VARCHAR(64),
    rejected_by_username  VARCHAR(64),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_insurance_claims_uid     UNIQUE (uid),
    CONSTRAINT uk_insurance_claims_no      UNIQUE (claim_no),
    CONSTRAINT ck_insurance_claims_claimed CHECK (claimed_amount >= 0),
    CONSTRAINT ck_insurance_claims_settled CHECK (settled_amount >= 0 AND settled_amount <= claimed_amount)
);
CREATE INDEX idx_insurance_claims_plan     ON insurance_claims(payer_plan_uid);
CREATE INDEX idx_insurance_claims_provider ON insurance_claims(provider_uid);
CREATE INDEX idx_insurance_claims_status   ON insurance_claims(status);
CREATE INDEX idx_insurance_claims_member   ON insurance_claims(membership_no);

-- (A) Back-reference on the source line (intra-billing -> numeric id FK): which
--     claim a COVERED line was rolled into. NULL = not yet claimed; a line is
--     claimable only while this is NULL.
ALTER TABLE invoice_line ADD COLUMN claim_id BIGINT;
ALTER TABLE invoice_line ADD CONSTRAINT fk_invoice_line_claim
    FOREIGN KEY (claim_id) REFERENCES insurance_claims(id);
-- Hot path: find unclaimed COVERED lines for a payer plan + member.
CREATE INDEX idx_invoice_line_claimable
    ON invoice_line(payer_plan_uid, membership_no)
    WHERE coverage_status = 'COVERED' AND claim_id IS NULL;

CREATE TABLE insurance_claim_lines (
    id               BIGSERIAL PRIMARY KEY,
    uid              VARCHAR(26)   NOT NULL,
    claim_id         BIGINT        NOT NULL,   -- intra-billing FK -> insurance_claims(id)
    invoice_line_id  BIGINT        NOT NULL,   -- intra-billing FK -> invoice_line(id)
    service_uid      VARCHAR(26),              -- cross-module snapshot -> masterdata service
    kind             VARCHAR(16)   NOT NULL,
    description      VARCHAR(255)  NOT NULL,
    quantity         NUMERIC(12,2) NOT NULL,
    unit_price       NUMERIC(14,2) NOT NULL,
    amount           NUMERIC(14,2) NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by  VARCHAR(80),
    updated_by  VARCHAR(80),
    version     BIGINT,
    CONSTRAINT uk_insurance_claim_lines_uid          UNIQUE (uid),
    CONSTRAINT uk_insurance_claim_lines_invoice_line UNIQUE (invoice_line_id),
    CONSTRAINT ck_insurance_claim_lines_amount       CHECK (amount >= 0),
    CONSTRAINT fk_insurance_claim_lines_claim        FOREIGN KEY (claim_id) REFERENCES insurance_claims(id),
    CONSTRAINT fk_insurance_claim_lines_invoice_line FOREIGN KEY (invoice_line_id) REFERENCES invoice_line(id)
);
CREATE INDEX idx_insurance_claim_lines_claim ON insurance_claim_lines(claim_id);
