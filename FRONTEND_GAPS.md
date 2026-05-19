# Frontend Gaps — `hmis-engine-web`

Inventory of pending frontend work after the W1 → W9 parity sweep (session 2026-05-19, backend Phases 38 → 47).

Each gap is independently pickable. **Hard parity gaps** are surfaces where the backend ships a capability and the UI has zero entry-point. **Polish** items are W1–W9 scope cuts. **Verification debt** is what was deferred because the CLI environment can only run `ng build`.

---

## ⏯ Resume checkpoint — 2026-05-19 (end of session)

**Branch:** `develop`, head `7024193`. Working tree clean. Pushed.

**Done this session (A1 → A2 → A3, all 3 hard parity gaps):**

| Card | Commit | Summary |
|---|---|---|
| A1 | `aad91d9` | Pharmacy WASTAGE write-off — extended `StockEditComponent` with a third `write-off` mode (reason `<select>` + positive qty), `writeOff()` on `StockService`, "Write off" button beside "Adjust" in stock-list rows |
| A3 | `fecc52a` | `MedicineUnit` dropdowns on receive / adjust / write-off / GRN — `MedicineService.listUnits()`, unit `<select>` defaulting to base, `unitUid` omitted when base chosen |
| A2 | `7024193` | Employee CRUD — new `features/hr/employee/` module (service + list + form + edit + detail + terminate modal + routes), nav link, HR default redirect now `/hr/employees`. W5-era inline `EmployeeReadService` removed; payroll now imports the full service. |

**Next up (start here tomorrow):**

1. **C1 — Browser smoke check** *(recommended next — surfaces bugs in everything just shipped before piling on more polish)*. Start `ng serve`, walk through W1–W9 + A1/A2/A3 against a running backend. The full checklist is in §C1 below.
2. **B1–B5 polish** (any order, all S effort). Each is independent. Pick opportunistically.
3. **C2 Karma/Jasmine specs** (L effort) — last, after surfaces have stabilised through C1.

**Open caveats carried forward:**
- A3 dropdowns hit `MedicineUnitController` which is gated on `MASTERDATA_MANAGE`. Same pre-existing constraint as the medicine search dropdown — pharmacists with only `PHARMACY_ACCESS` will 403. Fix is a backend `MASTERDATA_READ` privilege split, out of scope for this doc.

**No `ng serve` was run** this session. Only `ng build` (~5–6s, green at every commit).

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

- **Status:** TODO
- **Type:** polish (W2)
- **Effort:** S
- **Current:** create form is a textarea blob of order UIDs parsed on whitespace/comma
- **Plan:** typeahead/dropdown of unbatched `LAB_TEST` orders in `REQUESTED` state for the chosen lab-test type — multi-select with chips
- **Files:** `features/encounter/lab-batch/create-lab-batch.component.ts` + a new `orders/lab-order-read.service.ts` method `searchUnbatched(testTypeUid)`

### B2. Consumable stock adjust modal

- **Status:** TODO
- **Type:** polish (W4)
- **Effort:** S
- **Current:** uses `globalThis.prompt()` for delta + reason
- **Plan:** proper modal with reactive form (delta number, reason textarea, optional unit dropdown)
- **Files:** `features/consumables/stock/` — new `adjust-stock-modal.component.ts`

### B3. Pharmacy sale dispense — sales-pharmacy override modal

- **Status:** TODO
- **Type:** polish (W8)
- **Effort:** S
- **Current:** `pharmacy-sale-detail.dispenseLine` uses `globalThis.prompt()` for the optional `salesPharmacyUid`
- **Plan:** modal with a pharmacy dropdown (filtered to those the user has access to), defaulting to "no override"
- **Files:** `features/pharmacy/sale/pharmacy-sale-detail.component.ts` + new `dispense-line-modal.component.ts`

### B4. Follow-up of picker on start-consultation

- **Status:** TODO
- **Type:** polish (W6)
- **Effort:** S
- **Current:** `?followUpOf=<uid>` query-param wiring expects a raw 26-char ULID paste
- **Plan:** when patient is selected, dropdown of that patient's prior `COMPLETED` consultations (most recent first, top 10)
- **Files:** `features/encounter/consultation/start-consultation.component.ts` + `consultation.service.listForPatient(patientUid)` if not present

### B5. CASH-patient registration-fee gate UX

- **Status:** TODO
- **Type:** polish (W9)
- **Effort:** S
- **Current:** backend 4xx error surfaces as raw error string
- **Plan:** intercept the specific error code/message, show contextual alert: *"Settle the registration fee first"* with a deep-link to the registration invoice (use `invoice.service.findRegistrationFee` to resolve the link)
- **Files:** likely `features/encounter/consultation/start-consultation.component.ts` and/or `features/encounter/admission/admit-patient.component.ts` (wherever the CASH booking path lives)

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

Hard parity gaps (A1, A2, A3) are all done. Remaining order:

1. ~~**A1 Pharmacy write-off**~~ — DONE `aad91d9`
2. ~~**A3 MedicineUnit dropdowns**~~ — DONE `fecc52a`
3. ~~**A2 Employee CRUD**~~ — DONE `7024193`
4. **C1 Browser smoke check** ← *resume here* — covers W1–W9 and the new A1/A2/A3 surfaces
5. **B1–B5 polish** — pick opportunistically; none block each other
6. **C2 Component specs** — last; the surfaces should be settled before locking them down with tests

---

## E. Out of scope (do not implement here)

- **Statutory tax tables / allowance auto-prefill / worked-time auto-prefill on payroll** — deliberately deferred to a dedicated HR/finance product (Phase 47 decision)
- **Insurance-specific per-service price lists** — backend ships cross-cutting `ServicePrice` instead of legacy per-service tables (PROCESS.md ⚠️ row, accepted design simplification)
