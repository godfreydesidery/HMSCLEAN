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
| M4 | ~12 **role + patient-class + payment-gated** work queues (each = "my work, paid") | Collapsed into one generic `/encounters/orders` list (kind+status only) | High | 🚧 Phase 1 done (reception + pharmacy + nurse); ⬜ Phase 2 (lab/radiology/procedure) |
| M5 | Doctor's prescription lands in a **pharmacy dispensing queue** | No pharmacy queue endpoint existed; the dispense modal had to be handed a prescription | High | ✅ Phase 1D — `GET /encounters/prescriptions/worklist` + `DispenseWorklistComponent` feeding the dispense modal |
| M6 | Admission auto-routes to a **nurse queue**; admission linked to its consultation | No nurse queue; `admission.consultationUid` optional; nursing screens reached only by manual lookup | High | ✅ Phase 1E — `GET /encounters/admissions/nurse-worklist` + `NurseQueueComponent` |
| M7 | **PatientType drives routing/filtering** everywhere | Stored + shown as a badge, but never used to filter/route (only to gate outsider-direct orders) | Medium | ✅ Phase 1F — `PatientClassScope` (OUTPATIENT/INPATIENT/OUTSIDER) drives the new queues; inpatient = active admission |
| M8 | Lab/Radiology/Procedure each have outpatient/inpatient/outsider queues, payment-gated | Single generic worklist, no patient-class scope, no payment gate | Medium | ⬜ Phase 2 |
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
- **Phase 2:** M8 — evolve the generic `/encounters/orders` worklist into
  role + patient-class scoped lab/radiology/procedure queues reusing the same
  pattern.
- **Documented:** M11 (Visit), M12 (NonConsultation).

Tick items here as they land; mirror the result into `PROCESS.md` §17.
