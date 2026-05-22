# Frontend Gaps — `hmis-engine-web`

Inventory of pending frontend work after the W1 → W9 parity sweep (session 2026-05-19, backend Phases 38 → 47).

Each gap is independently pickable. **Hard parity gaps** are surfaces where the backend ships a capability and the UI has zero entry-point. **Polish** items are W1–W9 scope cuts. **Verification debt** is what was deferred because the CLI environment can only run `ng build`.

---

## ⏯ Resume checkpoint — 2026-05-20 (end of session)

**Branch:** `develop`, head `6f12463`. Working tree clean. **Not yet pushed** (`origin/develop` is at `7024193`).

**Done this session (B1 → B5, all five polish items):**

| Card | Commit | Summary |
|---|---|---|
| B2 | `80cf349` | Consumable stock adjust — replaced `globalThis.prompt()` with `AdjustConsumableModalComponent` (signed-delta reactive form + projected on-hand) |
| B3 | `80cf349` | Pharmacy dispense — replaced the salesPharmacyUid `prompt()` with `DispenseLineModalComponent` (pharmacy dropdown, defaults to the sale's own pharmacy) |
| B4 | `80cf349` | Start-consultation follow-up is now a dropdown of the patient's prior COMPLETED consultations (`recentForPatient`, top 10), loaded on patient-uid change; a pre-filled `?followUpOf` uid not in the list is preserved as an extra option |
| B5 | `80cf349` | CASH-patient unpaid-registration-fee 422 is intercepted and shown as a "Settle the registration fee first" alert with a deep-link to the registration invoice |
| B1 | `6f12463` | Lab batch member picker — **needed a new backend endpoint** (`GET /encounters/lab-batches/batchable-orders?labTestTypeUid=`) since none existed. Replaced the order-uid textarea blob with a checkbox table of eligible (REQUESTED, unbatched) orders. Spans backend + frontend (5 + 4 files). |

**Next up (start here next session):**

1. **`git push`** — this session's two commits (`80cf349`, `6f12463`) are local-only.
2. **C1 — Browser smoke check** *(recommended — surfaces bugs in everything shipped)*. Start `ng serve`, walk through W1–W9 + A1/A2/A3 + B1/B5 against a running backend. The full checklist is in §C1 below. **Also exercise the B1 picker and the B5 reg-fee gate while the backend is up.**
3. **C2 Karma/Jasmine specs** (L effort) — last, after surfaces have stabilised through C1.

**Open caveats carried forward:**
- A3 dropdowns hit `MedicineUnitController` which is gated on `MASTERDATA_MANAGE`. Same pre-existing constraint as the medicine search dropdown — pharmacists with only `PHARMACY_ACCESS` will 403. Fix is a backend `MASTERDATA_READ` privilege split, out of scope for this doc.

**B1 has an integration test.** `LabBatchIT` (existed since Phase 45 — `595d714`) gained `batchableOrdersListsEligibleAndDropsBatched`, which asserts the new `batchable-orders` endpoint lists REQUESTED unbatched CBC orders, excludes a different-test order, carries a renderable DTO, and drops an order once it's batched. **2/2 `LabBatchIT` tests green** (`mvn -Dtest=LabBatchIT`). *(Correction: an earlier note in this session wrongly claimed the lab-batch module had no IT — a glob/grep false-negative I should have verified; it has had one since Phase 45.)*

**No `ng serve` was run** this session. Backend `mvn compile` + `mvn -Dtest=LabBatchIT` (BUILD SUCCESS) for B1; frontend `ng build` green at every commit.

---

## Legend

- **Status**: `TODO` (not started) · `IN_PROGRESS` · `DONE`
- **Type**: `parity-gap` (backend ships, UI missing) · `polish` (W1–W9 scope cut) · `verification` (smoke / tests)
- **Effort**: `S` (≤ ½ day) · `M` (½ – 1 day) · `L` (> 1 day)

---

## A. Hard parity gaps

### A1. Pharmacy WASTAGE write-off UI

- **Status:** DONE
- **Type:** parity-gap
- **Effort:** M
- **Landed:** extended `StockEditComponent` with a third `write-off` mode (reason `<select>` + positive-only quantity), added `writeOff()` to `StockService`, added "Write off" button next to "Adjust" in the per-batch row action group. Disabled when batch quantity ≤ 0.
- **Backend surface (already shipped, Phase 37):**
  - `POST /pharmacy/pharmacies/uid/{uid}/stock/write-off`
  - Request: `{ medicineUid, batchUid?, quantity, unit?, reason, note? }`
  - `WastageReason` enum: `EXPIRED` · `DAMAGED` · `RECALLED` · `LOST` · `OTHER`
- **Frontend gap:**
  - No button anywhere in `features/pharmacy/` to trigger a write-off
  - No modal, no reason picker, no batch picker
- **Plan:**
  - Add `writeOff(pharmacyUid, payload)` to `pharmacy/stock.service.ts`
  - Create `features/pharmacy/stock/write-off-modal.component.ts` — Reactive form with medicine search, batch dropdown (optional), quantity, unit dropdown (W1 of MedicineUnit awareness — see A3), reason `<select>` populated from a `WASTAGE_REASONS` constant, note textarea
  - Wire a "Write off" button on the pharmacy stock list row dropdown
  - Surface success via toast/snackbar consistent with existing pharmacy patterns
- **Privilege gate:** `PHARMACY_ACCESS` (existing) — confirm whether legacy gates write-off on a stricter privilege (e.g. `PHARMACY_WRITE_OFF`) before merging
- **Acceptance:**
  - Modal opens from the stock list row
  - Submitting decrements the on-hand quantity for that batch
  - Wastage row shows up in the existing stock-movement history view
  - Reason field is required; `OTHER` requires a note

---

### A2. Employee CRUD UI

- **Status:** DONE
- **Type:** parity-gap
- **Effort:** L
- **Landed:** new `features/hr/employee/` feature module — full `EmployeeService` (search / findByUid / create / update / setStatus / terminate), 3-section employee form (identity / contact / employment), paginated list with name+no+designation+department+hireDate+status columns, detail page with status dropdown (ACTIVE / ON_LEAVE / SUSPENDED) + dedicated terminate modal (TERMINATED is one-way), edit page. Routed under `/hr/employees` with the HR default redirect now pointing there. Nav link added.
- **Refactor:** removed the W5-era inline `EmployeeReadService` and `EmployeeSummary` type from `payroll.service.ts`/`payroll.types.ts` — payroll-detail now imports the full `EmployeeService.search()` from the new feature, and `EmployeeSummary` is re-exported from `employee.types.ts` for compatibility.
- **Backend surface (Phase 26, already shipped):**
  - `GET /hr/employees` (paginated search)
  - `POST /hr/employees`
  - `GET /hr/employees/uid/{uid}`
  - `PUT /hr/employees/uid/{uid}`
  - `POST /hr/employees/uid/{uid}/terminate`
  - Other lifecycle endpoints (suspend / reinstate) — confirm in `EmployeeController` before scoping
- **Frontend gap:**
  - Only `EmployeeReadService.search` exists (built in W5 for the payroll picker)
  - No employee list, no form, no edit, no terminate flow
- **Plan:**
  - Promote `EmployeeReadService` → `EmployeeService` with full CRUD
  - `features/hr/employee/`:
    - `employee-list.component.ts` — paginated table, role + status filter, search by name/employee-no
    - `employee-form.component.ts` — create + edit (mirror patient-form 3-section pattern: identity / contact / employment)
    - `employee-detail.component.ts` — header + tabs (profile, employment history, terminate modal trigger)
    - `terminate-employee-modal.component.ts` — reason + effective date
  - Add route block under `features/hr/hr.routes.ts` (paths: `employees`, `employees/new`, `employees/:uid`, `employees/:uid/edit`)
  - Update `features/hr/hr.routes.ts` index redirect if employees is meant to be the landing tab
  - Nav link under HR menu group
- **Privilege gate:** `HR_ACCESS` for read; consider `HR_MANAGE` (if present) for write/terminate
- **Acceptance:**
  - Can create an employee, see them in the list, edit their profile, terminate them
  - Terminated employees no longer appear in the W5 payroll picker (verify `EmployeeReadService.search` already filters by status — if not, fix it)

---

### A3. `MedicineUnit` unit-awareness in pharmacy stock forms

- **Status:** DONE
- **Type:** parity-gap
- **Effort:** M
- **Landed:** added `MedicineUnit` type + `listUnits(medicineUid)` to `MedicineService`. Wired a unit `<select>` into `StockEditComponent` (receive/adjust/write-off — `prefillMedicineUid` carries the medicine from the row action), and one per-line on the GRN `ReceiveGoodsComponent`. Default-selects the base unit; `unitUid` is omitted from the payload when the base unit is chosen (backend converts via `factorToBase` otherwise).
- **Known limitation:** `MedicineUnitController` GET is gated on `MASTERDATA_MANAGE`; same pre-existing constraint applies to the medicine list dropdown. Out of scope for this card — fix is a backend privilege split (e.g. `MASTERDATA_READ`).
- **Backend surface (Phase 43):**
  - Manual receive, manual adjust, write-off (A1), and GRN ingestion all accept an optional `unit` field
  - Unit defaults to the medicine's `baseUnit` if omitted; non-base units are converted via `MedicineUnit.factor`
- **Frontend gap:**
  - Pharmacy stock **receive** form has no unit dropdown
  - Pharmacy stock **adjust** form has no unit dropdown
  - GRN **receive-line** form has no unit dropdown
  - (Write-off form: covered by A1 — build the unit dropdown there from day one)
- **Plan:**
  - Add `listUnits(medicineUid)` to `masterdata/medicine.service.ts` (returns `MedicineUnit[]` — `{ uid, name, factor, isBase }`) if not already present
  - In each affected form: after a medicine is picked, populate a unit `<select>` (default selected = baseUnit, label format `"<name> (×<factor>)"`)
  - Send `unit` (uid) when non-default
- **Affected files (verify paths during impl):**
  - `features/pharmacy/stock/receive-stock.component.ts`
  - `features/pharmacy/stock/adjust-stock.component.ts`
  - `features/procurement/grn/grn-receive-line.component.ts` (or equivalent)
- **Acceptance:**
  - Picking a non-base unit + qty `N` results in backend on-hand increasing by `N × factor` (base units)
  - The list view shows on-hand in base units (unchanged behaviour)

---

## B. W1–W9 polish (deliberate scope cuts)

### B1. Lab batch member picker

- **Status:** DONE (`6f12463`)
- **Type:** polish (W2) — **but required backend work** (re-scoped from S to M)
- **Effort:** S → M (no eligible-orders endpoint existed; had to add one)
- **Landed:** picking a lab test in `lab-batch-create.component` now loads its eligible orders (REQUESTED LAB_TEST orders for that type, not already in a batch) into a checkbox table (select all / clear, live selected count); submit sends the checked uids. Replaced the whitespace/comma order-uid textarea blob.
- **Backend added** (kept inside the lab-batch module so the order module stays dependency-free):
  - `ClinicalOrderRepository.findAllByKindAndServiceUidAndStatusOrderByRequestedAtAsc`
  - `LabBatchMemberRepository.findAllByOrderUidIn` (already-batched filter)
  - `LabBatchDtos.BatchableOrderDto` (orderNo + patient no/name + urgency + requestedAt)
  - `LabBatchService.listBatchable(labTestTypeUid)` — resolves patient name/no
  - `GET /encounters/lab-batches/batchable-orders?labTestTypeUid=` (gated `ENCOUNTER_ACCESS` via the controller class annotation)
- **Frontend:** `LabBatchService.listBatchable` + `BatchableOrder` type; multi-select table in `lab-batch-create`.
- **Test:** `LabBatchIT.batchableOrdersListsEligibleAndDropsBatched` (added this session) — 2/2 `LabBatchIT` green.

### B2. Consumable stock adjust modal

- **Status:** DONE (`80cf349`)
- **Type:** polish (W4)
- **Effort:** S
- **Landed:** new `adjust-consumable-modal.component.ts` — reactive form with a signed-delta (non-zero-integer validator) + reason note + a live "new on-hand" projection. `ConsumableStockComponent.adjust()` now opens it via `NgbModal` instead of `globalThis.prompt()`. (No unit dropdown — consumables have no unit-conversion model, unlike medicines.)

### B3. Pharmacy sale dispense — sales-pharmacy override modal

- **Status:** DONE (`80cf349`)
- **Type:** polish (W8)
- **Effort:** S
- **Landed:** new `dispense-line-modal.component.ts` — a pharmacy dropdown defaulting to "Default — <sale pharmacy>" (the opening pharmacy is excluded from override options). Returns the override uid or `null`; the detail component owns the dispense call so busy/refresh stay in one place. Replaced `globalThis.prompt()` in `pharmacy-sale-detail.dispenseLine`.

### B4. Follow-up of picker on start-consultation

- **Status:** DONE (`80cf349`)
- **Type:** polish (W6)
- **Effort:** S
- **Landed:** the follow-up field is now a `<select>` populated from `consultationService.recentForPatient(patientUid)` (filtered to COMPLETED, top 10), reloaded whenever a full 26-char patient uid is entered (debounced `valueChanges`). A `?followUpOf` uid pre-filled via query param but not in the candidate list is preserved as a synthetic option. `recentForPatient` already existed — no new service method needed.

### B5. CASH-patient registration-fee gate UX

- **Status:** DONE (`80cf349`)
- **Type:** polish (W9)
- **Effort:** S
- **Landed:** `start-consultation.submit()` now routes booking errors through `handleBookingError`. A 422 whose message matches `/registration fee/i` (the `RegistrationFeeListeners` gate — `ErrorCode.BUSINESS_RULE`) raises a contextual "Settle the registration fee first" warning alert and resolves the invoice via `invoiceService.findRegistrationFee(patientUid)` for an "Open registration invoice" deep-link to `/billing/<uid>`.
- **Note:** only the **consultation** booking path is gated server-side — admission does *not* consult the gate (confirmed in `ConsumableChartIT`), so `admit-patient` was left unchanged.

---

## C. Verification debt

### C1. Browser smoke check — W1 → W9

- **Status:** TODO
- **Type:** verification
- **Effort:** M
- **Current:** only `ng build` has been run for every W-phase
- **Plan:** start `ng serve`, walk every W1–W9 surface end-to-end against a running backend
- **Surfaces to click through:**
  - W1 HR assets — list / create / edit / detail / scan-by-tag
  - W2 Lab batches — create with blob, add/remove members, state transitions
  - W3 Order attachments — upload, download, delete from consultation-detail
  - W4 Consumables — issue from admission tab, stock receive, stock issue
  - W5 HR payroll — create period, upsert items, approve, mark paid, cancel
  - W6 Consultation follow-up & transfer — schedule follow-up, transfer between clinics
  - W7 Patient kin 3-contact, `lastVisitAt`, card-scan `/patients/by-no/`
  - W8 Multi-pharmacy dispense — filling vs sales pharmacy override
  - W9 Registration-fee card — both "Ensure fee" and "Open invoice" paths

### C2. Karma/Jasmine specs for W1 → W9 components

- **Status:** TODO
- **Type:** verification
- **Effort:** L
- **Current:** harness configured, zero spec files written for any W-phase component
- **Plan:** start with the highest-risk surfaces (W2 state transitions, W5 payroll state machine, W9 registration-fee ensure path), then back-fill list/form components
- **Approach:** mirror whatever spec pattern exists in the billing feature (the canonical pattern source for the codebase)

---

## D. Ordering recommendation

Hard parity gaps (A1–A3) and all polish (B1–B5) are done. Remaining order:

1. ~~**A1 Pharmacy write-off**~~ — DONE `aad91d9`
2. ~~**A3 MedicineUnit dropdowns**~~ — DONE `fecc52a`
3. ~~**A2 Employee CRUD**~~ — DONE `7024193`
4. ~~**B2–B5 polish**~~ — DONE `80cf349`
5. ~~**B1 lab batch picker**~~ — DONE `6f12463` (required a backend endpoint)
6. **`git push`** — `80cf349` + `6f12463` are local-only.
7. **C1 Browser smoke check** ← *resume here* — covers W1–W9, A1/A2/A3, and B1/B5
8. **C2 Component specs** — last; the surfaces should be settled before locking them down with tests. (Backend ITs are healthy — `LabBatchIT` now covers B1's `batchable-orders` query.)

---

## E. Formerly out-of-scope — revisited 2026-05-20

The user asked to revisit both E items. Findings + decisions:

- **Insurance-specific per-service price lists — ALREADY DELIVERED (no work).** The cross-cutting `ServicePrice(planUid, kind, serviceUid)` matrix covers all 7 priced kinds with a pricing UI at [masterdata/pricing/](hmis-engine-web/src/app/features/masterdata/pricing/), marked ✅ in PROCESS.md (lines 633/698/734). The original "out of scope" only meant *not* recreating the legacy's six separate `*InsurancePlan` tables — that single-table approach is a deliberate design-flaw fix ([[process-fidelity]]), and recreating the legacy tables would be a regression. **Decision: leave as-is.**

- **Payroll statutory tax tables / allowance + worked-time auto-prefill — BUILT this session (configurable, data-driven).** Legacy payroll is manual entry (no PAYE computation), so hard-coding statutory rates would both invent a flow and bake in rules I don't have. Instead built a **configurable band-table** model: `PayrollComponent` (EARNING/DEDUCTION × FIXED/PERCENT/BAND, base BASIC/GROSS) + `PayrollComponentBand` (progressive bands), a CRUD masterdata UI, and a stateless **compute** endpoint that auto-prefills a payroll item's gross/deductions from the active components for a given basic salary (with optional worked-days proration). Rates/bands are entered by HR — no statutory values are hard-coded. See §F.

---

## F. Configurable payroll components (built 2026-05-20)

**Status:** DONE · **Type:** feature (E-item 2) · **Effort:** L (full-stack)

A data-driven replacement for the deferred "statutory auto-prefill" cut. HR defines reusable earning/deduction rules; the payroll item form computes gross + deductions from them. **No statutory rates are in code — everything is data the hospital enters.**

**Model (no new migration — folded into `V48__hr_payroll.sql`, DB not yet created):**
- `PayrollComponent` — `code`, `name`, `type` (EARNING | DEDUCTION), `method` (FIXED | PERCENT | BAND), `base` (BASIC | GROSS), `fixedAmount`, `percentRate` (fraction), `active`, `sortOrder`.
- `PayrollComponentBand` — progressive rows (`fromAmount`, `toAmount` nullable = open top, `rate`) for `method = BAND`.

**Compute semantics** (`PayrollComponentService.compute`): basic → optional worked/period-day proration → earnings (on basic) → gross = basic + earnings → deductions (on basic or gross per component) → net. Each band's rate applies only to its slice of the base (standard progressive PAYE). FIXED amounts are flat (not pro-rated).

**Endpoints** (all `HR_ACCESS`):
- `GET/POST /hr/payroll/components`, `GET/PUT/DELETE /hr/payroll/components/uid/{uid}`, `PUT …/active`
- `POST /hr/payroll/compute` → `{ effectiveBasic, totalEarnings, grossPay, totalDeductions, netPay, lines[] }` (stateless; touches no payroll item)

**Frontend:**
- `features/hr/payroll/payroll-component-{list,form}.component.ts` (form has a FormArray band editor; rates entered as %). Nav: "Payroll setup" → `/hr/payroll/components`.
- Payroll-detail gains an "Auto-prefill from components" panel: enter basic (+ optional worked/period days) → Compute → breakdown shown and gross/deductions copied into the item form (still editable before save).

**Test:** `PayrollComponentIT.configuredComponentsDriveAutoPrefillCompute` — configures a FIXED allowance + PERCENT-of-gross + progressive BAND, asserts full-month and 15/30-day-prorated compute totals. Green. `mvn compile`/`ng build` green.
