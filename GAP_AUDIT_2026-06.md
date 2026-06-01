# Legacy ↔ New process & user-flow gap audit — 2026-06-01

Walked **7 patient-lifecycle journeys** through both systems (legacy `Zana-HMIS-API`
+ `zana-hmis` Angular vs. new `hmis-engine-api` + `hmis-engine-web`), simulating the
personas: outpatient walk-ins, outsiders (walk-in single service), inpatients who
are admitted → stay → discharged, deceased closures, and external referrals. Every
candidate gap was **adversarially re-verified against the new codebase** to drop
false "missing" alarms.

**Result: 38 confirmed gaps — 8 HIGH, 13 MEDIUM, 17 LOW. No false alarms.**
The rewrite is broadly faithful; the gaps cluster in **closure (discharge/referral/
death)**, **cashier receipts & reports**, **pay-before-service queue visibility**,
and **nursing charts**.

Severity is impact-on-the-journey, not effort. `CONFIRMED` = truly absent/divergent;
`PARTIAL` = partly present (noted).

---

## What is already at parity (so the picture is fair)
Registration/demographics/kin/payment-type, send-to-doctor → reception queue →
consultation authoring (vitals/notes/diagnosis/orders/prescriptions), the OUTSIDER
walk-in order+invoice pathway, lab/radiology/procedure accept-approve-result gates +
structured results + reference-range flagging + lab batches + attachments (clinician
side), pharmacy dispense lifecycle + multi-pharmacy + RO/TO/RN transfers + unit
conversion, admission + bed reserve/occupy + **deposit gate** + ward-day accrual +
nurse worklist + MAR + vitals/dressings/care-plan, the **discharge bill-clearance
gate** + separate-approver discharge plan, invoices/credit-notes/refunds, insurance
covered-routing + **claim ledger**, **patient-first cashier with line-level
check-to-pay** (just landed), cashier shift. Clinician-productivity reporting exists
(via HR roll-up, not a per-open metric).

---

## HIGH (8)

| Ref | Journey | Gap | Verdict |
|---|---|---|---|
| **DIAG-2** | Diagnostics | Unpaid orders are **visible & workable** in the tech queue (worklist defaults `settledOnly=false`); only final `complete()` is gated. Legacy hides unpaid orders from the queue entirely (PAID/COVERED, inpatient also VERIFIED). A tech can accept/run/enter results for an unpaid order. | CONFIRMED |
| **PHARM-2** | Pharmacy | Same class: dispense worklist defaults `settledOnly=false`, unpaid scripts are visible/advanceable, the `settled` flag isn't even shown as a badge; hard gate only at `markSold`. | CONFIRMED |
| **DISCH-1** | Closure | No **closure worklist** — `DischargePlanRepository` has no list/by-status query and no screen surfaces PENDING discharges/referrals/deaths to a second approver. Authoring + approval sit in one doctor-launched modal. | CONFIRMED |
| **DISCH-2** | Closure | No **printable closure document** (Discharge Summary / Referral Letter / Death record). The structured fields persist but render to no handable artefact; no pdf/print anywhere in the web app. | CONFIRMED |
| **DISCH-3** | Closure | **Outpatient death / external referral cannot be recorded** — closure is mounted only under an admission; `ConsultationStatus` has no DECEASED/REFERRED. Legacy records these via a consultation branch. | CONFIRMED |
| **ADMIT-1** | Inpatient | No **fluid-balance chart** (intake/output/drainage) anywhere — no field, endpoint, or tab. Core ICU/HDU record. | CONFIRMED |
| **BILL-1** | Cashier | No **POS receipt** after a cashier payment — only an on-screen banner. No print/pdf plumbing exists. | CONFIRMED |
| **BILL-2** | Cashier | No **collections / cash-up report** (per-cashier, date-range). Raw data exists (`Payment.createdBy` + `sumCashByUserInRange`) but no endpoint/screen. | CONFIRMED |
| **DIAG-1** | Diagnostics | No distinct **COLLECTED** specimen state / collect action / `collectedBy/At` audit — folded into ACCEPTED. HIGH only if specimen-receipt audit is a compliance need, else MEDIUM. | CONFIRMED |

## MEDIUM (13)
- **REG-1** Patient search has no **membership/insurance-card** lookup (legacy `load_patients_like_and_card`). One-line backend predicate + a search box.
- **REG-2** `changeType` OUTSIDER→OUTPATIENT doesn't sweep/block the patient's **open outsider orders + draft outsider invoice** → orphaned walk-in work. (REG-3 payment-type change shares this blind spot.)
- **OPC-1** Consultation **transfer** requires clinic+clinician up-front, books immediately, no pending-transfer queue, **no cancel/revert**.
- **OPC-2** Consultation **sign-out** never blocks on unpaid downstream bills — it always auto-cancels them (legacy *free* close), collapsing the guarded *referral* close path. No operator confirmation.
- **OPC-3** No **doctor-request → nurse-fill vitals** lifecycle / outpatient nurse-triage queue; vitals recorded inline, `record()` not gated to IN_PROGRESS.
- **ADMIT-2** No **daily care-activity record** (feeding/positioning/bed-bath/RBS/FBS).
- **DISCH-4** DECEASED closure **doesn't flag the patient** (no DECEASED PatientType) → a deceased patient stays active and re-bookable.
- **DISCH-5** Referral target is **free-text** `referralFacility`; legacy had an `ExternalMedicalProvider` masterdata FK.
- **BILL-3** No **patient-first inpatient till** — ADMISSION scope is deliberately excluded from the cashier (documented design choice); inpatient pays only via the whole-invoice admission screen.
- **BILL-4** No **direct-pending cash queue** / scope-payer filter on the invoice list / printable patient invoice. (Insurance side partly covered by the claims module.)
- **BILL-5** Revenue breaks down by **service-kind only**, not **payment-mode**; no aggregated **pharmacy-sales report**.
- **PHARM-1** No **select-working-pharmacy** session scoping (queue/stock not scoped to a chosen dispensary).

## LOW (17, mostly documented divergences / nice-to-haves)
REG-3 (payment-type change shares REG-2 blind spot, no auto-sign-out), REG-4 (no carry-over physical file-no), OPC-4 (no "my open consultations" doctor view), OPC-5 (no switch-follow-up-to-billable; no follow-up queue), OPC-6 (per-open clinician metric — productivity already via HR), DIAG-4 (per-role landing screens collapsed to one worklist; no deep-link presets), DIAG-5 (procedure gate relabel accept→approve, function intact + adds theatre/op-record), ADMIT-3 (ward price per-ward not per-type — reachable via "Prices" action; data-migration check only), ADMIT-4 (admit page vs inline 4-level picker), ADMIT-5 (no doctor ward-round view), DISCH-6 (bed freed at approval not authoring; no HELD state), DISCH-7 (self-approval hard-blocked — can stall solo-clinician sites; config-gate it), BILL-6 (combined reg+consultation pay split into two tills — pre-tick to restore one-click), BILL-7 (one Payment/invoice vs per-line Collection ledger — reconstructable), PHARM-3 (no bulk "dispense all checked"), PHARM-4 (single SKU domain vs Item↔Medicine coefficient — equivalent), PHARM-5 (no reusable retail customer master / POS receipt / archive).

---

## Execution plan (one branch + PR each, off main, tests green)
1. **Nursing charts** — fluid-balance (ADMIT-1) + care-activity (ADMIT-2) entities/endpoints/tabs. **✅ DONE (PR #36).**
2. **Closure foundation** — `ExternalMedicalProvider` masterdata (DISCH-5) + outpatient death/referral (DISCH-3) + flag deceased patient (DISCH-4). **✅ DONE (branch `closure-foundation`):** unified `DischargePlan` keyed off admission OR consultation (`ClosureSubject`), `ConsultationStatus.DECEASED/REFERRED` + `/encounters/consultations/uid/{uid}/closure`, `md_external_medical_provider` masterdata + referral FK-by-uid, `Patient.deceased` flag set on any DECEASED approval + book/admit guards. Migration V73; FE external-providers page + consultation closure modal + deceased badge.
3. **Closure worklist** (DISCH-1) + config-gated self-approval (DISCH-7). **✅ DONE (branch `closure-worklist`):** single `GET /encounters/closures/worklist?subjectType=` queue of PENDING closure plans across both subjects (admission + consultation) with patient/subject deep-link info; `hmis.closure.allow-self-approval` flag (default false) relaxes the author≠approver four-eyes block for solo-clinician sites. FE closure-worklist page + nav. ClosureWorklistIT + ClosureSelfApprovalConfigIT.
4. **Print plumbing** — reusable receipt/document service → POS receipt (BILL-1) + closure documents (DISCH-2) + printable invoice (BILL-4).
5. **Reports** — collections/cash-up (BILL-2) + revenue-by-payment-mode + pharmacy-sales (BILL-5).
6. **Pay-before-service visibility** — class-aware queue default (DIAG-2 + PHARM-2) + paid/unpaid badge. *(Note: must keep inpatient VERIFIED orders visible — not a blanket `settledOnly=true`.)*
7. **Registration** — membership/card search (REG-1) + outsider sweep/block on type/payment change (REG-2/3).
8. **Consultation flow** — transfer queue/cancel (OPC-1), sign-out bill guard (OPC-2), vitals lifecycle (OPC-3) — confirm intended semantics first.
9. LOW cluster + documented-divergence notes.
