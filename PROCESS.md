# Zana-HMIS — Process Reference

> This document captures the **operational processes** of the legacy Zana-HMIS
> system, which has been in production use for ~4 years. The new clean-arch
> rewrite (`hmis-engine-api` + `hmis-engine-web`) is to **inherit the same
> business processes** — the workflows, status transitions, and cross-role
> handoffs are proven and must not change. What changes is the architecture
> and UI: clean module boundaries, modern stack, no design flaws of the
> legacy data model, no Angular template anti-patterns.
>
> **Sources of truth for the process:**
> - Legacy backend: `d:\My_Works\HMS\ZANAHMIS-2-feature\Zana-HMIS-API\api\api\src\main\java\com\orbix\api\`
> - Legacy frontend: `d:\My_Works\HMS\ZANAHMIS-2-feature\zana-hmis\src\app\pages\`
> - Legacy writeup: `d:\My_Works\HMS\ZANAHMIS-2-feature\HMIS System Writeup.docx`
>
> **Scope of this document:** the proven processes by role, the status enums
> they pass through, and the cross-role handoffs. The last section is a
> coverage map — legacy process → new-system status — that drives the
> remaining build phases.

---

## Table of contents

1. [Vocabulary and core concepts](#1-vocabulary-and-core-concepts)
2. [Registration](#2-registration)
3. [Doctor — outpatient + inpatient](#3-doctor--outpatient--inpatient)
4. [Nurse](#4-nurse)
5. [Laboratory](#5-laboratory)
6. [Radiology](#6-radiology)
7. [Procedure](#7-procedure)
8. [Pharmacy](#8-pharmacy)
9. [Store](#9-store)
10. [Procurement](#10-procurement)
11. [Payments / Billing](#11-payments--billing)
12. [Human Resource](#12-human-resource)
13. [Management / Reports](#13-management--reports)
14. [Admin / master data](#14-admin--master-data)
15. [Status enums (legacy domain)](#15-status-enums-legacy-domain)
16. [Unusual / domain-specific mechanics](#16-unusual--domain-specific-mechanics)
17. [Coverage map — legacy → new system](#17-coverage-map--legacy--new-system)

---

## 1. Vocabulary and core concepts

| Term | Meaning |
|---|---|
| **Patient** | A registered person on the patient registry. Has a unique file no, payment type, optional insurance plan. |
| **Outpatient** | Patient flowing through a clinic consultation. The default path. |
| **Outsider** | Walk-in patient. Bypasses the clinic / consultation flow and can directly request lab, radiology, procedures, or pharmacy sales. Must still be on the patient registry. |
| **Consultation** | A doctor's encounter with a patient at a clinic. The container for clinical notes, diagnoses, orders, prescriptions. |
| **Admission** | An inpatient stay. Anchors progress notes, daily ward charges, and discharge planning. Triggered from a consultation. |
| **Bill / Invoice** | The financial counterparts of every billable encounter (consultation, lab, radiology, procedure, medicine, ward stay). |
| **Insurance plan** | A payer-side contract that drives per-service pricing overrides. Every billable service may have a plan-specific price; otherwise the cash price applies. |
| **Pharmacy** | A dispensing location inside the hospital. Multiple pharmacies can exist (OPD pharmacy, IPD pharmacy, theatre pharmacy …). Each has its own stock. |
| **Store** | Central inventory warehouse (separate from pharmacies). Stock flows: supplier → store → pharmacy → patient. |
| **RO / TO / RN** | The three transfer documents used inside pharmacy / store inventory: Request Order, Transfer Order, Receive Note. See §8 and §16. |
| **GRN** | Goods Received Note — receipt against a Local Purchase Order (LPO). |
| **LPO** | Local Purchase Order — placed on a supplier. |

---

## 2. Registration

The entry point for **every** clinical workflow. A patient must be on the
registry (and have paid the registration fee, for cash patients) before
anything else can happen.

**Pages:** patient-list, patient-register.

**Workflow:**

1. **Lookup** — receptionist searches by file no, name, or pre-generated
   search key (partial match supported). Card-scan-style lookup also supported.
2. **Create** — if not found, register the patient. Required: first name,
   last name, gender, date of birth, payment type. Optional: middle name,
   phone, email, address, kin information (up to three contacts), nationality.
3. **Payment type** — CASH, INSURANCE, DEBIT_CARD, CREDIT_CARD, MOBILE. If
   INSURANCE, the insurance plan and membership number are mandatory.
4. **Registration bill** — system creates an UNPAID registration bill. For
   cash patients this must be PAID before consultation can begin.
5. **Patient type** — OUTPATIENT or OUTSIDER. Drives later routing; can be
   flipped on an existing patient (e.g. an outpatient becomes an outsider for
   a one-off lab request).
6. **Last visit** — registration display surfaces the patient's last visit
   timestamp for continuity.

**Cross-role handoffs:**
- → Cashier (if CASH and registration unpaid)
- → Doctor (if OUTPATIENT, after registration paid)
- → Lab / Radiology / Procedure / Pharmacy (if OUTSIDER, direct)

---

## 3. Doctor — outpatient + inpatient

**Pages:** my-consultation, list-from-reception, patient-history-menu,
clinical-note-history, working-diagnosis-history, final-diagnosis-history,
prescription-history, lab-test-history, radiology-history, procedure-history,
general-examination-history, doctor-inpatient-list, doctor-inpatient,
doctor-follow-up, follow-up-list, doctor-cracking, discharge-plan,
deceased-note, referral-plan.

### 3.1 Outpatient consultation lifecycle

1. **PENDING** — created when registration routes the patient to a clinic.
2. **IN_PROCESS** — doctor opens the consultation. From here on, the doctor
   adds clinical notes, examination, diagnoses, prescriptions, orders.
3. **COMPLETED** — doctor concludes the visit. Closes for further data entry
   on the consultation itself (orders/prescriptions raised earlier are still
   actionable downstream).
4. **CANCELLED** — abandoned before completion.

### 3.2 Inside a consultation

- **Clinical notes** — free-text narrative per visit (chief complaint,
  history, exam, assessment, plan).
- **General examination** — structured vitals + findings.
- **Working diagnosis** — differential list while the doctor is still
  investigating.
- **Final diagnosis** — definitive list, drives downstream coding/reports.
- **Prescriptions** — medicine + dose + frequency + route + duration +
  quantity. Status flow detailed in §8.
- **Lab orders**, **Radiology orders**, **Procedure orders** — see §5–§7.
- **Follow-up flag** — marks a visit as a follow-up to a prior visit (affects
  billing and chart continuity).
- **Consultation transfer** — patient can be freed from one clinic/clinician
  and routed to another. Original consultation kept; transfer record links
  to the new one.

### 3.3 Inpatient (admission)

1. **Admit** — doctor admits patient from a consultation. Bed + ward assigned.
2. **Daily care** — doctor enters daily notes; nurses run the chart (see §4).
3. **Discharge plan** — doctor authors a structured discharge document:
   history, investigation, management, operation note, ICU note,
   recommendations. Status: PENDING → APPROVED (by ward administrator).
4. **Discharge** — admission closed (status DISCHARGED). Final bill reconciled.
5. **Deceased note** — alternative closure if the patient died.
6. **Referral plan** — alternative closure if patient referred out.

### 3.4 Cross-role handoffs

- → Lab / Radiology / Procedure (orders raised in the consultation)
- → Pharmacy (prescriptions raised in the consultation)
- → Nurse (admission)
- → Cashier (consultation fee, order fees, prescription fees)
- → Ward administrator (discharge plan approval)

---

## 4. Nurse

**Pages:** nurse-outpatient-list, nurse-inpatient-list, nurse-outsider-list,
nurse-outpatient-chart, nurse-inpatient-chart, nurse-outsider-chart,
nursing-history-menu, nurse-patient-history-menu, plus history equivalents
of every doctor view.

**Workflow:**

1. **Admission reception** — nurse admits the patient to a bed and records
   admission vitals.
2. **Patient observation chart** — vitals (BP, temp, pulse, respiration,
   SpO2) entered at scheduled intervals. Multiple entries per admission.
3. **Nursing care plan** — goals + interventions per admission.
4. **Nursing progress notes** — per-shift narrative notes (separate from
   doctor's notes).
5. **Patient consumable chart** — dressings, gauze, IV fluids, etc.
   consumed during the stay. Each entry decrements store/pharmacy stock and
   accrues a charge.
6. **Patient dressing chart** — wound dressing record (type, date, wound
   status).
7. **Prescription handling** — nurse picks up dispensed meds from pharmacy
   and administers; records administration times.
8. **Discharge coordination** — removes lines, provides education, checks
   discharge plan is APPROVED before patient leaves.

**Cross-role handoffs:**
- ← Doctor (admission, prescriptions, orders)
- → Pharmacy (drug administration pull)
- → Store / Pharmacy (consumable charting decrements inventory)

---

## 5. Laboratory

**Pages:** lab-outpatient-list, lab-inpatient-list, lab-outsider-list,
lab-patient-list, lab-test, patient-results, reports.

**Workflow:**

1. **Order arrives** — LabTest created by doctor (or directly from
   registration for an OUTSIDER). Status **PENDING**.
2. **Accept** — lab tech accepts the order; specimen collected. Status
   **ACCEPTED**.
3. **Process and report** — analysis performed; results entered against
   each line of the test type (level, unit, range, description). Long-text
   narrative supported. Files / images can be attached.
4. **Complete** — Status **COMPLETED**. Result visible to doctor and
   patient.
5. **Batch processing** — for high-volume identical tests on the same date,
   `PrescriptionBatch` groups them so the tech can process as a batch.
6. **Billing** — lab test bill accrues at order time, priced from
   `LabTestType` (cash) or `LabTestTypeInsurancePlan` (insurance).

**Cross-role handoffs:**
- ← Doctor / Registration (order)
- → Doctor (results)
- → Cashier (lab bill)

---

## 6. Radiology

**Pages:** radiology, radiology-outpatient-list, radiology-inpatient-list,
radiology-outsider-list.

**Workflow:**

1. **Order** — doctor orders by radiology type (X-ray, US, CT, MRI). Status
   **PENDING**.
2. **Schedule + accept** — patient slotted; status **ACCEPTED**.
3. **Acquire** — images / video captured; attached to the radiology record
   (multiple attachments per study).
4. **Report** — radiologist authors written report.
5. **Complete** — Status **COMPLETED**.
6. **Billing** — priced from `RadiologyType` or `RadiologyTypeInsurancePlan`.

**Cross-role handoffs:**
- ← Doctor / Registration (order)
- → Doctor (images + report)
- → Cashier (radiology bill)

---

## 7. Procedure

**Pages:** patient-procedure, procedure-outpatient-list,
procedure-inpatient-list, procedure-outsider-list.

**Workflow:**

1. **Order** — doctor orders by procedure type; flags whether theatre is
   required. Status **PENDING**.
2. **Theatre scheduling** (theatre procedures only) — date, time, theatre
   assigned.
3. **Approve** — surgeon / anaesthesia sign off. Status **APPROVED**.
4. **Operative record** — findings, technique, instruments, complications
   documented during/after.
5. **Complete** — Status **COMPLETED**.
6. **Billing** — priced from `ProcedureType` or `ProcedureTypeInsurancePlan`.
7. **Post-op** — nursing and doctor handle wound care, pain, mobilisation
   via the admission chart.

**Cross-role handoffs:**
- ← Doctor (order)
- → Theatre team (scheduling)
- → Nurse (post-op care)
- → Cashier (procedure bill)

---

## 8. Pharmacy

The most complex role-area. Handles patient prescriptions, retail / OTC
sales, inventory management, batch tracking, and inter-pharmacy /
pharmacy-store transfers (the famous **RO/TO/RN** three-document dance).

**Pages:** select-pharmacy, pharmacy-outpatient-list, pharmacy-inpatient-list,
pharmacy-outsider-list, patient-pharmacy, pharmacy-medicine-stock-status,
pharmacy-sales-order, pharmacy-sales-order-list, pharmacy-to-pharmacy-r-o,
pharmacy-to-pharmacy-r-o-list, pharmacy-to-pharmacy-t-o,
pharmacy-to-pharmacy-r-n, pharmacy-to-store-r-o, store-to-pharmacy-r-n,
reports.

### 8.1 Prescription dispensing

1. **PENDING** — created by doctor.
2. **ACCEPTED** — pharmacist picks up.
3. **HELD** — awaiting payment (cash) or stock.
4. **VERIFIED** — quality / clinical check passed.
5. **APPROVED** — ready to dispense.
6. **SOLD** — given to patient. Stock decremented from the dispensing
   pharmacy. Bill marked PAID (if cash + paid up front) or routed to
   insurance.
7. **REJECTED** / **CANCELLED** — not dispensed.

For **cash** patients each prescription line has a payStatus: UNPAID → PAID.
The pharmacist will not move to SOLD if UNPAID.

### 8.2 Retail / OTC sales order

For OUTSIDER patients and walk-in retail customers — no doctor prescription
required. Same status lifecycle on each `PharmacySaleOrderDetail` as in §8.1.

### 8.3 Stock model

- **PharmacyMedicine** — what each pharmacy carries.
- **PharmacyMedicineBatch** — batch + expiry tracking per pharmacy.
- **PharmacyStockCard** — append-only ledger of every movement (receipt,
  dispense, transfer, wastage, adjustment).
- **Conversion coefficients** — `ItemMedicineCoefficient` lets the same
  medicine be tracked at different units in different locations (carton in
  the store, blister in the pharmacy). Conversion factor applied on
  movement.

### 8.4 Pharmacy → Pharmacy transfer (the RO/TO/RN dance)

1. **Requesting pharmacy** creates a **PharmacyToPharmacyRO** (Request
   Order). Status: PENDING → VERIFIED → APPROVED → SUBMITTED → IN_PROCESS
   → GOODS_ISSUED → COMPLETED. Or REJECTED / RETURNED.
2. **Delivering pharmacy** confirms by creating a **PharmacyToPharmacyTO**
   (Transfer Order) listing exactly what is shipping. Mirror status lifecycle.
3. **Receiving pharmacy** completes a **PharmacyToPharmacyRN** (Receive
   Note) confirming actual quantities and batch IDs received. Status:
   PENDING → COMPLETED. Each line links to the source batch, applying any
   conversion coefficient (e.g. 1 vial → 5 doses).
4. Stock cards on both sides update only when the RN is COMPLETED.

### 8.5 Pharmacy ↔ Store transfer

Same shape as §8.4 but spans pharmacy ↔ central store:
- **PharmacyToStoreRO** — pharmacy requests stock from store, or pharmacy
  returns surplus to store (direction encoded in status semantics).
- **StoreToPharmacyTO** — store prepares the shipment.
- **StoreToPharmacyRN** — pharmacy confirms receipt.

### 8.6 Multi-pharmacy operations

`PharmacySaleOrderDetail` tracks **issuePharmacy** (where the prescription
was filled) and **salesPharmacy** (where stock was actually pulled from).
This lets a prescription be filled at pharmacy A but use stock from pharmacy
B without an explicit transfer.

---

## 9. Store

**Pages:** select-store, item-inquiry, goods-received-note,
store-item-stock-status, store-to-pharmacy-t-o, pharmacy-to-store-r-o-list,
conversion-coefficients, reports.

**Workflow:**

1. **Goods receipt** — incoming stock from procurement (see §10) lands in
   `StoreItemBatch` rows on GRN approval.
2. **Pharmacy resupply** — store fulfils PharmacyToStoreROs by creating
   StoreToPharmacyTOs; receiving pharmacy confirms via RN.
3. **Reverse flow** — pharmacy may return surplus stock to store (same
   entities, status differentiates intent).
4. **Direct consumption** — nursing consumables (dressings, fluids) may be
   issued directly from store to ward, decrementing `StoreItemBatch`.
5. **Conversion coefficients** — store may receive in bulk units (carton)
   and ship to pharmacies in smaller units (blister) — coefficients applied
   on transfer.
6. **Item inquiry** — staff lookup of current stock + batch + expiry across
   all batches of an item.

---

## 10. Procurement

**Pages:** local-purchase-order, supplier-item-price-list, reports.

**Workflow:**

1. **Supplier setup** — vendors registered with contact, tax, address.
2. **Supplier item price list** — per-supplier pricing for each item;
   supports comparison shopping (ItemSupplier links items to multiple
   suppliers).
3. **Local Purchase Order (LPO)** — draft → PENDING → VERIFIED (procurement
   manager) → APPROVED (director) → SUBMITTED (sent to supplier) → RECEIVED
   (goods arrived) → REJECTED / RETURNED.
4. **Goods Received Note (GRN)** — created from an APPROVED LPO when goods
   arrive. Lists ordered qty vs. received qty per line (may differ due to
   shortages / damage). Per-line batch info captured
   (`GoodsReceivedNoteDetailBatch`). Status: PENDING → VERIFIED (QC passed)
   → APPROVED (accepted) → REJECTED.
5. **Inventory update** — GRN approval creates / updates `StoreItemBatch`
   in the destination store.
6. **Three-way match** — supplier invoice qty matched against PO qty and
   GRN qty before payment release.

---

## 11. Payments / Billing

**Pages:** registration-payment, patient-payment, lab-test-payment,
investigation-payment, medication-payment, procedure-payment,
radiology-payment, inpatient-payment, patient-invoice, patient-direct-invoices,
patient-insurance-invoices, deceased-list, discharge-list, referral-list.

**Workflow:**

1. **Charge accrual** — every billable event (registration, consultation,
   lab order, radiology order, procedure order, medicine dispense, daily
   ward stay) writes a `PatientBill` line that rolls up into the patient's
   `PatientInvoice`.
2. **Invoice status** — PENDING (still accruing) → PARTIALLY_PAID → PAID, or
   CANCELLED (write-off).
3. **Payment** — `PatientPayment` records cash / card / mobile money /
   insurance settlement, with `PatientPaymentDetail` allocating to specific
   invoice lines.
4. **Insurance routing** — for insured patients, lines route to the
   `InsurancePlan`. Insurance-specific pricing tables exist for every
   billable service (`*InsurancePlan` family). If no plan-specific price,
   default (cash) price is used or the service is denied (depending on
   config).
5. **Inpatient daily charge accrual** — every admitted day, ward charge
   auto-added to the admission's invoice. Priced via `WardType` or
   `WardTypeInsurancePlan`.
6. **Collections** — `Collection` entity tracks cash actually collected at
   the cash desk, distinct from invoice amounts. End-of-day reconciliation
   matches the two.
7. **Credit notes** — `PatientCreditNote` records partial / full write-offs
   (hardship, hospital policy, error correction). Reduces patient's total owing.
8. **Refunds** — overpayments handled via negative `PatientPaymentDetail` or
   credit note. No dedicated refund entity.

---

## 12. Human Resource

**Pages:** employee-register, payroll, asset-register.

**Workflow:**

1. **Employee register** — staff personal + employment details. Links to a
   `User` account for system access.
2. **Payroll** — period-based payroll runs with gross / deductions / net.
   `PayrollDetail` itemises tax, insurance, loan, etc.
3. **Clinician performance** — `ClinicianPerformance` rolls up
   consultations, procedures, lab tests, patient feedback per clinician —
   feeds incentive calculations.
4. **Asset register** — fixed assets (equipment, furniture) tracked.
5. **Role & privilege management** — roles map to job functions; privileges
   gate per-module access. User ← role ← privileges.

---

## 13. Management / Reports

**Pages:** management-dashboard, collections-report, doctors-reports,
ipd-report, patient-report, revenue-report, report-template.

**Process:**

- **Operational dashboard** — patient census, bed occupancy, today's
  revenue, claims status, stock-outs.
- **Financial reports** — revenue by source (registration, consultation,
  pharmacy, lab, radiology, procedure, ward stay), outstanding invoices,
  insurance claims status, cash collected vs. billed.
- **Clinical reports** — clinician case load, procedure / lab / radiology
  volumes, average length of stay.
- **Pharmacy reports** — inventory levels, stock-outs, expired stock,
  supplier lead times, COGS.
- **Quality reports** — lab turnaround, imaging report time.

---

## 14. Admin / master data

**Pages:** company, insurance-management, inventory, medical-operations,
medical-units, personnel, reports, stakeholders, user-and-access.

**Process:**

- **Company profile** — hospital name, address, contact, logo.
- **Medical units** — clinics, wards (+ bed types + beds), theatres,
  pharmacies, stores.
- **Inventory masterdata** — medicines, consumables, conversion coefficients.
- **Medical operations** — lab test types (+ ranges), radiology types,
  procedure types, diagnoses, dosages, routes, frequencies.
- **Stakeholders** — suppliers, payers (insurance providers), partners.
- **Insurance management** — insurance plans + per-service pricing
  overrides for every billable service kind.
- **Personnel** — employees, roles, designations.
- **User & access** — user accounts, role assignment, privilege wiring.
- **Pricing** — consultation fees per clinic, medicine prices,
  lab/radiology/procedure prices, ward day rates, with insurance-specific
  overrides on each.

---

## 15. Status enums (legacy domain)

Reference list of every state constant. The new system must preserve the
**semantic intent** of these states. Naming is allowed to be cleaner
(e.g. `IN_PROGRESS` vs. legacy `IN-PROCESS`) but the lifecycle gates must
match.

| Entity | States |
|---|---|
| **Consultation** | PENDING → IN_PROCESS → COMPLETED / CANCELLED |
| **Admission** | ADMITTED → DISCHARGED (other terminal states: DECEASED, REFERRED via referral plan) |
| **LabTest** | PENDING → ACCEPTED → COMPLETED / CANCELLED |
| **Radiology** | PENDING → ACCEPTED → COMPLETED / CANCELLED |
| **Procedure** | PENDING → APPROVED → COMPLETED / CANCELLED |
| **Prescription (Rx)** | PENDING → ACCEPTED → HELD → VERIFIED → APPROVED → SOLD ; REJECTED / CANCELLED |
| **Prescription pay status** | UNPAID → PAID |
| **PharmacySaleOrderDetail** | same as Rx |
| **PharmacyToPharmacy RO / TO** | PENDING → VERIFIED → APPROVED → SUBMITTED → IN_PROCESS → GOODS_ISSUED → COMPLETED ; REJECTED / RETURNED |
| **PharmacyToPharmacy RN** | PENDING → COMPLETED |
| **PharmacyToStore RO / TO / RN** | same as their P2P equivalents |
| **GoodsReceivedNote** | PENDING → VERIFIED → APPROVED ; REJECTED / RETURNED / SUBMITTED |
| **LocalPurchaseOrder** | PENDING → VERIFIED → APPROVED → SUBMITTED → RECEIVED ; REJECTED / RETURNED |
| **DischargePlan** | PENDING → APPROVED |
| **PatientInvoice** | PENDING → PARTIALLY_PAID → PAID ; CANCELLED |
| **PatientBill** | UNPAID → PAID |
| **PaymentType** | CASH / INSURANCE / DEBIT_CARD / CREDIT_CARD / MOBILE |
| **PatientType** | OUTPATIENT / OUTSIDER |

---

## 16. Unusual / domain-specific mechanics

These are the bits a generic HMIS rewrite tends to miss. They are **not**
optional — they have ridden through 4 years of production use.

1. **OUTSIDER (walk-in) patient pathway** — bypasses consultation. Labs,
   radiology, procedures, and pharmacy sales can be raised directly against
   the patient with no clinic / clinician routing. Used for one-off lab
   requests, pharmacy retail, occupational health screens.

2. **RO / TO / RN three-document transfer dance** — pharmacy ↔ pharmacy
   and pharmacy ↔ store inventory movements use three sequenced documents:
   Request Order (requester), Transfer Order (shipper), Receive Note
   (receiver). Each has its own status lifecycle. Stock cards update only
   when the RN is COMPLETED. Required for traceability and to handle
   in-transit losses.

3. **Conversion coefficients** — `ItemMedicineCoefficient` lets the same
   item be tracked at different units in different locations (carton in
   store, blister in pharmacy, tablet at point of dispense). Applied on
   every movement so the math stays consistent across the chain.

4. **Dual pricing per service (cash + per-plan)** — every billable service
   has a default (cash) price plus 0..N insurance-plan-specific prices via
   a separate `*InsurancePlan` table per service kind (medicine, lab,
   radiology, procedure, ward, consultation). Plan-specific takes precedence
   over cash; if neither exists, service may be denied or default applied
   (configurable).

5. **Inpatient daily charge accrual** — the admission bill grows on a
   schedule, not on a single event. One ward-day charge per day of stay,
   priced by ward type with insurance-specific overrides.

6. **Pharmacy sales order (retail OTC)** — pharmacy can sell medicines
   without a doctor prescription, for OUTSIDER patients or registered patients
   on a self-paid basis. Same status lifecycle as a prescription detail.

7. **Multi-pharmacy operations** — `issuePharmacy` (where the prescription
   was filled) and `salesPharmacy` (where the stock was actually pulled
   from) can differ on the same line. Avoids forcing an explicit
   PharmacyToPharmacy transfer for small-volume cross-pharmacy fulfilment.

8. **Ward-to-ward transfer** — an admitted patient can move between wards
   without closing the admission. Bed assignment updates; the admission
   record persists.

9. **Consultation transfer** — outpatient can be moved between clinics /
   clinicians mid-visit. Original consultation kept; new consultation in
   the target clinic; transfer record links them.

10. **Credit notes + refunds via signed amounts** — no dedicated Refund
    entity. Overpayments / write-offs handled via `PatientCreditNote` or
    negative `PatientPaymentDetail`.

11. **Three-way match for procurement** — invoice payment release requires
    LPO qty = GRN qty = supplier invoice qty.

---

## 17. Coverage map — legacy → new system

What the clean-arch rewrite (`hmis-engine-api` + `hmis-engine-web`)
currently delivers, mapped against the legacy process. Phases referenced
below are the git commits on `develop`.

Legend: ✅ covered · ⚠️ partial — needs work · ❌ not yet started

### 17.1 Registration

| Process | Status | Notes |
|---|---|---|
| Patient registry (create, edit, search, deactivate) | ✅ | Patient module from earlier phase. |
| Patient kin / nationality fields | ⚠️ | Only basic kin captured; legacy supports 3 kin contacts. |
| Payment type at registration (CASH, INSURANCE, etc.) | ✅ | `PaymentType` enum present. |
| Insurance plan + membership no at registration | ✅ | `insurancePlanUid` on Patient. |
| Registration fee bill | ❌ | New system charges consultation-level fees; no separate registration fee yet. |
| OUTPATIENT vs. OUTSIDER patient type | ❌ | Only one patient type today. Need to add and propagate through encounter and pharmacy. |
| Patient type conversion | ❌ | Depends on above. |
| Last visit tracking display | ⚠️ | Data is queryable but not surfaced on the registry. |
| Pre-generated search keys / card scan | ❌ | Plain name + no. search only. |

### 17.2 Doctor — outpatient

| Process | Status | Notes |
|---|---|---|
| Consultation lifecycle | ✅ | New: BOOKED → IN_PROGRESS → COMPLETED / CANCELLED. Legacy: PENDING → IN_PROCESS → COMPLETED / CANCELLED. Equivalent semantics; rename internally is acceptable. |
| Clinical notes (SOAP) | ✅ | Phase 1. |
| Working + final diagnoses | ✅ | Phase 1 — uses kind = WORKING / FINAL. |
| Lab / radiology / procedure orders | ✅ | Phase 2 — polymorphic ClinicalOrder. |
| Order results (narrative + impression + finalize) | ✅ | Phase 5. |
| Prescriptions | ⚠️ | Phase 2 — simplified status (REQUESTED / DISPENSED / CANCELLED). **Must be expanded** to PENDING → ACCEPTED → HELD → VERIFIED → APPROVED → SOLD plus pay-status. |
| Follow-up visit flag | ❌ | |
| Consultation transfer between clinics | ❌ | |

### 17.3 Doctor — inpatient + nurse

| Process | Status | Notes |
|---|---|---|
| Admission lifecycle | ✅ | Phase 4 — ADMITTED → DISCHARGED / DECEASED / TRANSFERRED / CANCELLED. |
| Ward / bed assignment | ✅ | Bed labels (free text) on the admission; legacy uses a `WardBed` entity for true bed availability. |
| Ward-to-ward transfer (in-stay) | ✅ | `transferWard()` on Admission. |
| Progress notes (per-shift) | ✅ | Phase 6 — kinds DOCTOR / NURSING / OBSERVATION / HANDOVER. |
| Patient observation chart (vitals across stay) | ✅ | Phase 22b — `AdmissionVitalsEntry` series at `/encounters/admissions/uid/{uid}/vitals`, immutable rows, per-shift / per-round trend. |
| Nursing care plan | ✅ | Phase 22b — `NursingCarePlanItem` per problem with goal + intervention + evaluation, ACTIVE → RESOLVED / CANCELLED. |
| Patient consumable chart | ❌ | Requires inventory link from ward issue to pharmacy/store. |
| Patient dressing chart | ✅ | Phase 22b — `DressingChartEntry` series with `WoundStatus` enum (CLEAN, HEALING, GRANULATING, SLOUGHY, INFECTED, NECROTIC, DEHISCED) + dressing applied. |
| Discharge plan (structured) | ✅ | Phase 22a — `DischargePlan` aggregate with structured fields (history, investigation, management, op note, ICU note, recommendations) + PENDING → APPROVED → drives admission closure on approval. Free-text `Admission.dischargeSummary` becomes a back-pointer to the plan. |
| Deceased note / referral plan | ✅ | Phase 22a — same `DischargePlan` aggregate with `kind = DECEASED` (requires timeOfDeath + causeOfDeath) or `REFERRAL` (requires referralFacility + referralReason). Approval routes the admission to DECEASED / TRANSFERRED. |

### 17.4 Laboratory

| Process | Status | Notes |
|---|---|---|
| Order acceptance + result entry | ✅ | Phase 5 via OrderResult. |
| Status flow (PENDING / ACCEPTED / COMPLETED / CANCELLED) | ✅ | Aligned with legacy. |
| Result attachments (files / images) | ❌ | No file upload yet. |
| Batch processing for high-volume tests | ❌ | |
| Insurance-specific lab pricing | ⚠️ | `ServicePrice` table covers it but only one row per (plan, service); legacy has a dedicated `LabTestTypeInsurancePlan`. Same data, different shape — acceptable. |

### 17.5 Radiology

| Process | Status | Notes |
|---|---|---|
| Order + accept + report | ✅ | Same OrderResult pipeline. |
| Image attachments | ❌ | Same gap as lab attachments. |
| Insurance-specific radiology pricing | ⚠️ | Same as 17.4. |

### 17.6 Procedure

| Process | Status | Notes |
|---|---|---|
| Order + procedure note (impression) | ✅ | Phase 5. |
| Theatre scheduling | ✅ | Phase 24 — `Theatre` masterdata + `ClinicalOrder.theatreUid` / `scheduledAt` / `scheduledByUsername` + `POST /encounters/orders/uid/{uid}/schedule`. Only valid for PROCEDURE-kind orders. |
| Operative record fields | ✅ | Phase 24 — `OperativeRecord` 1:1 with the procedure order: findings, technique, instruments, complications, specimens, surgical team, anaesthesia, start/end times. Upsert + lock workflow at `/encounters/orders/uid/{uid}/operative-record`. |

### 17.7 Pharmacy

| Process | Status | Notes |
|---|---|---|
| Per-pharmacy stock balance + movement ledger | ✅ | Phase 7. |
| Receive stock (RECEIPT movement) | ✅ | Phase 7 + Phase 8 via GRN. |
| Adjustment movement | ✅ | Phase 7. |
| Dispense to prescription (decrement) | ✅ | Phase 7 — pessimistic-locked. |
| Full prescription status lifecycle (PENDING → ACCEPTED → HELD → VERIFIED → APPROVED → SOLD) | ❌ | Currently REQUESTED → DISPENSED only. |
| Prescription pay-status | ❌ | |
| Pharmacy sales order (retail / OTC) | ❌ | No separate PharmacySaleOrder entity. |
| Pharmacy → Pharmacy transfer (RO / TO / RN) | ✅ | Phase 20b — requesting pharmacy RO → delivering pharmacy TO → requesting pharmacy RN, FEFO TRANSFER_OUT / TRANSFER_IN movements, shares the `TransferDocStatus` / `ReceiveNoteStatus` enums in `transfer.common.domain`. |
| Pharmacy ↔ Store transfer (RO / TO / RN) | ⚠️ | Phase 20a: forward direction only — pharmacy RO → store TO → pharmacy RN, with FEFO store-side issue and per-batch propagation to the pharmacy. Reverse-direction (pharmacy returns to store) deferred. |
| Conversion coefficients on items | ❌ | Single unit per medicine today. |
| Batch + expiry tracking per pharmacy | ❌ | Stock balance is a single integer per (pharmacy, medicine); no batch granularity. |
| Wastage / transfer-in / transfer-out movement kinds | ⚠️ | Enum has them but no flows emit them yet. |
| `issuePharmacy` vs. `salesPharmacy` split | ❌ | |

### 17.8 Store

| Process | Status | Notes |
|---|---|---|
| Central store as separate inventory | ❌ | Today, "pharmacy" stock is the only inventory; GRNs deposit into a pharmacy directly. Legacy puts goods into the store first, then transfers to pharmacy. |
| Store stock card + batches | ❌ | |
| Item inquiry across batches | ❌ | |
| Direct consumable issue to ward | ❌ | |

### 17.9 Procurement

| Process | Status | Notes |
|---|---|---|
| Supplier registry | ✅ | Phase 8. |
| Local Purchase Order (header + lines) | ✅ | Phase 8 + 23a — full legacy gate chain: DRAFT → VERIFIED → APPROVED → ORDERED → PARTIALLY_RECEIVED → RECEIVED, with REJECTED from any pre-submission state and CANCELLED from any non-RECEIVED state. |
| Goods Received Note | ✅ | Phase 8 + 23a — full PENDING → VERIFIED → APPROVED workflow. Stock credit + PO line `recordReceipt` now fire on APPROVED (not on creation), so a count mismatch caught at verification doesn't pollute the ledger. REJECTED branch has no stock impact. |
| Per-line batch info on GRN | ❌ | Single qty per line today; no batch breakdown. |
| Supplier item price list | ✅ | Phase 23b — `SupplierItemPrice` per (supplier, medicine, validity window). CRUD at `/procurement/suppliers/uid/{uid}/prices`; comparison shopping at `/procurement/medicines/uid/{uid}/prices/{active|best}`. LPO line still takes its own typed unit cost — the price list is a lookup, not auto-fill. |
| Three-way match (PO vs. GRN vs. invoice) | ❌ | Supplier invoice entity missing. |

### 17.10 Payments / Billing

| Process | Status | Notes |
|---|---|---|
| Invoice for consultation | ✅ | Phase 3. |
| Invoice for admission (ward-day accrual) | ✅ | Phase 6 — computed on demand from admit/discharge timestamps. Legacy accrues per-day on a schedule. Functionally equivalent; either model works. |
| Per-line type breakdown (CONSULTATION / LAB / PROCEDURE / RADIOLOGY / MEDICINE / WARD) | ✅ | Phase 3 + 6. |
| Invoice status (DRAFT → ISSUED → PARTIALLY_PAID → PAID / CANCELLED) | ✅ | Phase 3. Equivalent semantics to legacy. |
| Payment recording (multiple methods, partial allocation) | ✅ | Phase 3. |
| Insurance-specific pricing | ⚠️ | Via the cross-cutting `ServicePrice` table; legacy uses per-service tables. Acceptable design simplification — must verify all 6 service kinds have entries. |
| Credit note / write-off | ✅ | Phase 25 — `CreditNote` aggregate per invoice with `CreditNoteReason` (HARDSHIP / GOODWILL / ERROR_CORRECTION / SERVICE_NOT_RENDERED / ROUNDING / OTHER). Invoice gains `totalCredited`; `balance = subtotal - totalPaid - totalCredited`. POST `/billing/invoices/uid/{uid}/credit-notes`. |
| Refunds | ✅ | Phase 25 — `Refund` aggregate per invoice with `RefundReason` (OVERPAYMENT / SERVICE_NOT_RENDERED / DOUBLE_PAYMENT / CANCELLATION / OTHER) + `PaymentMethod`. Reduces `totalPaid` and rolls invoice status back from PAID → PARTIALLY_PAID / ISSUED as needed. POST `/billing/invoices/uid/{uid}/refunds`. |
| End-of-day cash collection vs. invoice reconciliation | ❌ | |
| Registration / consultation fee that gates clinical activity for cash patients | ⚠️ | Invoices exist but workflow does not block consultation if unpaid. |

### 17.11 Human Resource

| Process | Status | Notes |
|---|---|---|
| Employee register | ✅ | Phase 26 — `Employee` aggregate in `hr.employee.*` with optional 1:1 link to `iam.User`. CRUD at `/hr/employees`, gated by `HR_ACCESS`. Designation + department are strings for V1 (upgrade to masterdata later if needed). |
| Payroll | ❌ | Deferred — large business surface area. |
| Clinician performance | ✅ | Phase 26 — `GET /hr/employees/uid/{uid}/clinician-performance?from=&to=` rolls up consultations + admissions + lab/radiology/procedure orders for the linked username in a date range. |
| Asset register | ❌ | |

### 17.12 Management / Reports

| Process | Status | Notes |
|---|---|---|
| Operational dashboard | ✅ | Phase 12 — live KPIs + recent activity. Limited to counts; no revenue / inventory yet. |
| Revenue by source | ✅ | Phase 27 — `GET /reporting/revenue?from=&to=` returns total billed / collected / credited / refunded + per-`InvoiceLineKind` breakdown. |
| Patient register / IPD register | ✅ | Phase 27 — `GET /reporting/ipd-register?from=&to=&wardUid=&status=` returns the admissions list with ward + patient + clinician columns. |
| Bed occupancy | ✅ | Phase 27 — `GET /reporting/bed-occupancy` returns per-ward capacity + currently-occupied + available. |
| Pharmacy stock-out / expired report | ✅ | Phase 27 — `GET /reporting/stock-out?threshold=N` (default 0 = true stock-outs across pharmacies + stores) and `GET /reporting/expiring-batches?daysAhead=N` (default 30). |
| Clinician case load | ✅ | Phase 26 clinician-performance endpoint covers this. |

### 17.13 Admin / master data

| Process | Status | Notes |
|---|---|---|
| Company profile | ✅ | Phase 28 — singleton `CompanyProfile` at `/masterdata/company-profile` (GET for any authenticated caller; PUT gated by `MASTERDATA_MANAGE`). |
| Clinics, wards (+ types), pharmacies, stores | ⚠️ | Clinics, wards, pharmacies, stores all done. Bed-availability (per-bed entity with FREE/OCCUPIED status) still absent — could be a future polish phase. |
| Theatres | ✅ | Phase 24 — `Theatre` masterdata with full CRUD at `/masterdata/theatres`. Two sample theatres seeded. |
| Medicines, lab tests, radiology, procedures, diagnoses | ✅ | Masterdata phase. |
| Medicine units (base + alternates with conversion factors) | ✅ | Phase 21 — `MedicineUnit` aggregate, CRUD at `/medicines/uid/{uid}/units`, EACH base auto-seeded; transfer chains accept per-line `unitUid` and convert at the boundary. |
| Consumables | ✅ | Phase 28 — `Consumable` masterdata at `/masterdata/consumables`. Wiring into a ward-issue path is a follow-up. |
| Insurance plans + per-service pricing | ✅ | `InsurancePlan` + `ServicePrice` matrix. |
| Dosages / routes / frequencies dropdowns | ✅ | Phase 28 — three masterdata aggregates (`Dosage`, `AdministrationRoute`, `DosingFrequency`) at `/masterdata/{dosages|administration-routes|dosing-frequencies}`. Standard routes (ORAL/IV/IM/SC/TOPICAL/INHALED) and frequencies (OD/BD/TDS/QID/STAT/PRN with `timesPerDay`) seeded. Wiring picklists into `Prescription` is a follow-up. |
| Users + roles + privileges | ✅ | Phases 9–11. |

---

### 17.14 Build-phase plan to close the gap

Roughly grouped by dependency / coherence — each bullet is a self-contained
phase that respects the modulith boundaries:

1. **Patient type + OUTSIDER pathway** — add `PatientType` (OUTPATIENT /
   OUTSIDER), allow lab / radiology / procedure / pharmacy-sale to be
   raised on an outsider without a consultation. Add registration fee.
2. **Prescription status lifecycle + pay-status** — extend prescription
   states and gate dispensing on payment status for cash patients.
3. **Pharmacy retail (PharmacySaleOrder)** — OTC sales head entity, same
   line-level lifecycle as prescriptions.
4. **Batch + expiry tracking per pharmacy** — split `StockBalance` into
   per-batch rows; add expiry tracking; FEFO dispensing.
5. **Store domain** — separate central store with its own balances /
   ledger. Procurement GRN now lands in store, not pharmacy. Pharmacies
   must request from store.
6. **RO / TO / RN documents** — pharmacy ↔ store transfer chain first
   (depends on §5), then pharmacy ↔ pharmacy. Phase 20a delivered the
   forward P↔S chain (resupply); Phase 20b delivered the full P↔P chain.
   Both run without conversion coefficients (single unit per medicine).
   Reverse-direction P↔S (pharmacy returns to store) deferred.
7. **Conversion coefficients on items** — needed before RO/TO/RN to be
   useful in practice. Phase 21 delivered `MedicineUnit` (base + alternates
   with `factorToBase`) and wired it into both transfer chains. Stock
   balances stay in base units; conversion happens at the API boundary.
   Manual receive / adjust, dispense, retail sale and GRN still take base
   units directly (no unit awareness yet — can move in a follow-up phase).
8. **Structured discharge plan + nursing chart** — observation chart
   series, nursing care plan, consumable chart, structured discharge plan
   with APPROVED state, deceased + referral structured notes. Phase 22a
   delivered the unified `DischargePlan` aggregate (DISCHARGE / DECEASED
   / REFERRAL kinds) with the PENDING → APPROVED gate that drives the
   admission closure. Phase 22b added the observation chart
   (`AdmissionVitalsEntry`), nursing care plan
   (`NursingCarePlanItem`) and dressing chart
   (`DressingChartEntry`). Consumable chart still pending — needs a
   ward→pharmacy issue path first.
9. **Procurement gates + supplier price list** — add VERIFIED / APPROVED
   PO states, per-supplier item catalog. Phase 23a delivered the LPO
   gate chain (DRAFT → VERIFIED → APPROVED → ORDERED → ...) and the
   matching GRN workflow (PENDING → VERIFIED → APPROVED with stock
   credit deferred to APPROVED). Phase 23b added `SupplierItemPrice`
   with supplier-anchored CRUD and medicine-anchored comparison
   shopping. Three-way match (PO vs GRN vs supplier invoice) still
   deferred — no supplier invoice entity yet.
10. **Theatre + procedure scheduling** — theatre entity, scheduled date /
    time on procedure orders, operative-record fields. Phase 24 delivered
    all three: `Theatre` masterdata, scheduling fields on `ClinicalOrder`,
    and the structured `OperativeRecord` aggregate with surgeon /
    anaesthetist / timing fields.
11. **Credit notes, refunds, collections** — billing extensions. Phase 25
    delivered credit notes + refunds with the invoice settlement
    semantics (totalCredited + rollback on refund). End-of-day cash
    collection reconciliation still pending.
12. **HR module** — employees, payroll, clinician performance. Phase 26
    delivered the `Employee` aggregate and the live clinician-performance
    roll-up. Payroll + asset register deferred.
13. **Reporting** — revenue, IPD register, stock-out, clinician case load.
    Phase 27 delivered live read-only reports for all five: revenue
    summary (with per-kind breakdown), IPD register, bed occupancy,
    stock-out (pharmacy + store), expiring batches. Clinician case load
    was already covered by Phase 26 clinician-performance.
14. **Master data polish** — company profile, theatres, consumables,
    dosage / route / frequency dropdowns, bed availability. Phase 28
    delivered the company profile (singleton), consumables, and the
    three drug-administration lookups (dosage, route, frequency) with
    sensible seed data. Theatres were already done in Phase 24. Bed
    availability (per-bed entity with assignment) still pending — it's
    a bigger schema change that can be its own phase.

Items above are roughly ordered by dependency. A few independent ones
(structured discharge plan, credit notes, theatre scheduling) can slot in
out of order if they're more valuable for the next demo.

---
