# Zana-HMIS — Process Fidelity Mismatches & Remediation

> Companion to [PROCESS.md](PROCESS.md). `PROCESS.md` is the canonical map of
> the legacy workflows; this file tracks where the clean-arch rewrite
> **diverged from those proven workflows** and the remediation that restores
> fidelity. The legacy system is the process baseline — we change architecture
> and UI, not the business process.

The trigger: an audit (2026-05-21) found that, although `PROCESS.md`'s coverage
table marked every capability ✅, the **end-to-end operational flow** had
diverged. The connective tissue between roles — auto-created consultations,
role-scoped work queues, payment gates — was missing or replaced with manual,
search-and-paste steps.

## Status legend
- ✅ fixed · 🚧 in progress · ⬜ planned · 📝 documented difference (intentional)

---

## Mismatch inventory

| # | Legacy behavior (proven) | As-built divergence | Severity | Status |
|---|---|---|---|---|
| M1 | Receptionist "Send to doctor" **auto-creates** the consultation (PENDING) + consultation bill | Consultation is a separate manual screen; receptionist must re-search the patient, pick clinic+clinician; nothing auto-created | High | ✅ Phase 1A — "Send to doctor" modal on the patient + `ConsultationBookedEvent` → consultation-fee invoice |
| M2 | Doctor works a **"from reception" queue** of their PENDING consultations | No reception queue; only a generic consultation list with an optional clinician filter | High | ✅ Phase 1C — `GET /encounters/consultations/reception-queue` + `ReceptionQueueComponent` |
| M3 | Consultation bill created at send-to-doctor; doctor sees/opens only when **PAID/COVERED** | No consultation-fee invoice at booking; opening was ungated; the only gate sat on the *registration* fee at *booking* time | High | ✅ Phase 1B/1C — `ConsultationFeeService` seeds an ISSUED invoice; `Consultation.feeSettled` gate on `start()`; registration booking-block removed |
| M4 | ~12 **role + patient-class + payment-gated** work queues (each = "my work, paid") | Collapsed into one generic `/encounters/orders` list (kind+status only) | High | ✅ Phase 1 (reception + pharmacy + nurse) + Phase 2 (lab/radiology/procedure scoped by `kind` + `patientClass`) |
| M5 | Doctor's prescription lands in a **pharmacy dispensing queue** | No pharmacy queue endpoint existed; the dispense modal had to be handed a prescription | High | ✅ Phase 1D — `GET /encounters/prescriptions/worklist` + `DispenseWorklistComponent` feeding the dispense modal |
| M6 | Admission auto-routes to a **nurse queue**; admission linked to its consultation | No nurse queue; `admission.consultationUid` optional; nursing screens reached only by manual lookup | High | ✅ Phase 1E — `GET /encounters/admissions/nurse-worklist` + `NurseQueueComponent` |
| M7 | **PatientType drives routing/filtering** everywhere | Stored + shown as a badge, but never used to filter/route (only to gate outsider-direct orders) | Medium | ✅ Phase 1F — `PatientClassScope` (OUTPATIENT/INPATIENT/OUTSIDER) drives the new queues; inpatient = active admission |
| M8 | Lab/Radiology/Procedure each have outpatient/inpatient/outsider queues, payment-gated | Single generic worklist, no patient-class scope, no payment gate | Medium | ✅ Phase 2 — `/encounters/orders` now takes `kind` (role lens) + `patientClass` + `settledOnly`; `ClinicalOrder.settled` (V51) flipped by `SettlementDispatcher`; UI patient-class filter |
| M9 | Follow-up consultation bill = **NONE** (waived/free) | Follow-up flag existed; no fee waiver wired | Medium | ✅ Phase 1B — `ConsultationFeeService` waives the fee (zero) for follow-ups, settled at booking |
| M10 | Insurance consultation = **COVERED** at creation (doctor opens immediately) | No COVERED concept | Medium | ✅ Phase 1B — non-CASH treated as settled by the queue + gate |
| M11 | Legacy `Visit` parent groups same-day consultations/non-consultations | No `Visit` concept (consultation stands alone) | Low | 📝 deferred — assess need before reintroducing |
| M12 | Outsider gets an on-demand `NonConsultation` container | Outsider orders/Rx attach directly to the patient (`consultationUid=null`) | Low | 📝 kept — functionally equivalent design simplification |

---

## Remediation design (the cross-cutting decisions)

**`settled` flag pattern.** Because the `encounter` module must not depend on
`billing` (modulith boundary), payment status reaches the queues/gates via
event-driven denormalization: encounter aggregates carry a local `settled`
flag set by a **billing-side** dispatcher (billing → encounter is the legal
direction) when the relevant invoice is fully settled. Queues/gates read only
the local flag — never billing. CASH is gated on the flag; non-CASH is treated
as settled (legacy "COVERED").

**Billing gate move (M3).** The registration-fee-at-booking block was removed
(the registration invoice is still seeded and collected at the cashier). The
gate that matters — legacy-faithfully — is now the **consultation fee**: the
doctor's reception queue shows PAID/COVERED only, and opening a consultation is
refused for a CASH patient until the consultation fee is settled.

---

## Phase plan

- **Phase 1 (this effort):** M1, M2, M3, M5, M6, M7, M9, M10 — the flagship
  register→send-to-doctor→reception-queue flow with the consultation-fee gate,
  plus the pharmacy dispensing queue and nurse admission queue, on a shared
  patient-class scope.
- **Phase 2 (done):** M8 — the generic `/encounters/orders` worklist now scopes
  by `kind` (the per-role lens) + `patientClass` + `settledOnly`, reusing the
  `settled`-flag + `PatientClassScope` pattern. `ClinicalOrder.settled` (V51) is
  flipped by the same `SettlementDispatcher`.
- **Documented:** M11 (Visit), M12 (NonConsultation).

Tick items here as they land; mirror the result into `PROCESS.md` §17.

---

## Second pass — system-wide audit (2026-05-21)

A wider sweep (clinical orders, nursing, pharmacy/store, billing, procurement, HR)
beyond the registration→consultation→queue handoffs. Verdicts below were spot-checked
against current code. The headline is **M13**: the legacy "pay before the service is
rendered" rule for CASH patients is not enforced anywhere — the deeper non-adherence.

| # | Legacy behavior (proven) | As-built divergence | Severity | Status |
|---|---|---|---|---|
| **M13** | **Payment before service for CASH:** each lab/radiology/procedure order and each prescription gets an UNPAID bill at order time; the tech/pharmacist **cannot process or dispense** until it is PAID/COVERED | **No gate.** `StockService.dispense()` checks only `APPROVED` + stock; `ClinicalOrder.markInProgress/complete` have no payment check. A CASH patient's labs/meds can be delivered unpaid (revenue leak). The `settled` flag (Phase 1) only *filters* queues, it doesn't *block*. | **High** | ✅ Phase 3D (consultation path) — orders/prescriptions are billed onto the consultation invoice at order time (`ServiceChargeService` via `ClinicalOrderRaisedEvent`/`PrescriptionRaisedEvent`); `ClinicalOrder.complete()` + `Prescription.markSold()` refuse a CASH consultation item until its invoice is settled (non-CASH/zero settled at billing). 📝 OUTSIDER retail keeps the generate-then-pay-at-counter model (cashier-enforced). |
| **M14** | **Procedure approval gate:** PENDING → APPROVED (surgeon/anaesthetist sign-off) → COMPLETED | `ClinicalOrderStatus` has no `APPROVED`; any user can move a procedure REQUESTED → IN_PROGRESS → COMPLETED. Operative record is a sibling aggregate that doesn't gate completion. | **High** | ✅ Phase 3A — added `APPROVED`; `ClinicalOrder.approve()` (REQUESTED→APPROVED) required before a procedure can be worked; `POST /encounters/orders/uid/{uid}/approve` |
| **M15** | **Nursing drug administration (MAR):** nurse records actual dose given + time + patient response per administration (`PatientPrescriptionChart`) | No equivalent entity. Only pharmacy `dispensedAt` exists; the actual bedside administration is untracked. | **High** | ✅ Phase 3C — `MedicationAdministration` aggregate (V53) + `GET/POST /encounters/admissions/uid/{uid}/medication-administrations`; "Medications (MAR)" tab on admission detail with a record-dose modal that picks the admission's prescriptions |
| **M16** | **Lab/radiology accept step:** PENDING → ACCEPTED (specimen collected / study scheduled+accepted) → COMPLETED | Single coarse lifecycle (REQUESTED → IN_PROGRESS → COMPLETED) and `complete()` allows REQUESTED → COMPLETED directly — the specimen-custody / schedule-accept marker is lost. | Medium | ✅ Phase 3A — added `ACCEPTED`; `accept()` (REQUESTED→ACCEPTED) required for lab/radiology; `complete()` now only from IN_PROGRESS (no REQUESTED→COMPLETED skip); `POST .../accept` |
| **M17** | **Discharge requires an APPROVED discharge plan** before the admission can close | `DischargePlan` has the PENDING → APPROVED gate, but `AdmissionService.discharge()` doesn't require an approved plan — it's bypassable by calling discharge directly. | Medium | ✅ Phase 3B — `AdmissionService.discharge/markDeceased/transferOut` require an APPROVED matching-kind plan; `DischargePlanService.approve()` routes closure through `AdmissionService` (unified gate + bed release); new discharge-plan UI on admission detail (author → a *different* user approves) |
| **M18** | Procurement segregation of duties: VERIFY (manager) vs APPROVE (director) | LPO/GRN keep both states, but every endpoint is gated by one `PROCUREMENT_ACCESS` privilege — no role split. (Legacy also used a broad privilege, so this is a control *enhancement* opportunity.) | Medium | ✅ Phase 3E — new `PROCUREMENT_VERIFY` / `PROCUREMENT_APPROVE` privileges (seeded; ROOT keeps both); PO + GRN verify/approve endpoints gated method-level; PROCUREMENT role gets VERIFY, MANAGEMENT gets APPROVE |
| **M19** | Payroll DRAFT → VERIFIED → APPROVED → PAID | Current is DRAFT → APPROVED → PAID — the VERIFIED checkpoint is collapsed. | Medium | ✅ Phase 3E — added `VERIFIED` (V54); `PayrollPeriod.verify()` (DRAFT→VERIFIED, items lock) then `approve()` requires VERIFIED; `POST /hr/payroll/periods/uid/{uid}/verify`; Verify button on payroll detail |
| **M20** | Conversion coefficients applied on **every** stock movement | ~~Not applied on transfer/dispense~~ | Medium | ✅ already covered — **audit was wrong**: the RO/TO/RN chains DO convert (`UnitConversionService.toBaseQuantity` on all three docs, `PharmacyStoreTransferService` lines 111/196/329). Dispense is base-units **by design** (you prescribe N tablets, dispense N tablets — no carton-at-dispense). |
| **M21** | RO/TO/RN: stock moves **only when the RN is COMPLETED** | Current decrements at TO-issue (TRANSFER_OUT) and credits at RN (TRANSFER_IN). Audit trail intact, narrower in-transit window. | Low | 📝 acceptable simplification |
| **M22** | Insurance: per-service plan pricing **plus** claim submission / pre-auth / COVERED routing | Pricing only (`ServicePrice` matrix). No claim entity, pre-auth, or claims reconciliation lane. | Medium | 📝 already covered — **legacy has NO claims aggregate/workflow**: "register claim" there just means adding a `PatientBill` line to the `PatientInvoice`. The insurance invoice (COVERED lines + plan) *is* the claim — already modelled by `Invoice` + `ServicePrice` + non-CASH=COVERED. A separate claims/pre-auth module would invent a flow the legacy never had. |
| **M23** | Ward-day charge accrues daily (interim billing point mid-stay) | Daily `@Scheduled` `AdmissionAccrualJob` rebuilds each ADMITTED invoice's ward-day line to *now* and re-arms the discharge gate. | Low | ✅ done — the prior "on-demand is enough" verdict was **invalid**: nothing *triggered* on-demand generation, so a stay could be discharged with 0 ward-days billed. Now `AdmissionAdmittedEvent` → `AdmissionFeeListeners` seeds+issues the ward-bed invoice **at admit** (arming `bills_cleared`), and `AdmissionAccrualJob` accrues daily; `generateForAdmission` rebuilds while DRAFT/ISSUED-unpaid (frozen once paid). |
| **M24** | Payroll `PayrollDetail` itemises tax/insurance/loan deductions | Snapshot gross/deductions/net only; `PayrollComponent`/`Band` tables exist but aren't wired into the period flow. | Medium | ✅ done — `PayrollItemLine` (V55) persists the per-component breakdown on each item; upsert accepts `lines`; the payroll-detail auto-prefill carries the computed basic + earnings + deductions through; breakdown shown under each item row. |
| **M25** | `ClinicianPerformance` persisted + feeds incentive calculations | Computed on-demand (read-only); not persisted, not linked to payroll incentives. | Low | 📝 partly deliberate |

**Confirmed MATCH / acceptable (not gaps):** prescription state machine (full PENDING→SOLD chain enforced), retail sale-order lifecycle, multi-pharmacy issue/sales split, FEFO batch+expiry+wastage, central store + RO/TO/RN three docs, three-way match (PO/GRN/supplier invoice), supplier price list, GRN per-line batch (denormalised), credit notes/refunds (richer than legacy), cashier-shift reconciliation, bed model, dosage/route/frequency picklists, masterdata coverage, nursing vitals/care-plan/progress-notes/consumable-chart/dressing-chart, deceased/referral notes.

**Recommended fix order:** clinical control gates first (M14 procedure approval, M16 order lifecycle, M17 discharge enforcement, M15 MAR), then the revenue gate (M13, with its billing-model decision), then finance/procurement controls (M19, M18, M24, M25). M20–M25 marked 📝 are scope/judgement calls to confirm before building.
