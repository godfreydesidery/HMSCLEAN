# Plan: Staff ↔ Clinic relationships (clinician affiliation & consultation routing)

> **Status: IMPLEMENTED (2026-05-21).** Clinician⇄Clinic (Phases 1–3), the
> consultation booking/transfer hard gate, StorePerson⇄Store (Phase 4a), and the
> clinician provider profile (Phase 4b) are all built. Radiology/lab/theatre were
> verified to be role-scoped in *both* the legacy and the rewrite, so they were
> intentionally left unchanged (see R-table). Integration tests are written but
> require Docker (Testcontainers) to run; backend compiles and the modulith
> boundary check passes; the frontend builds. Companion to `PROCESS_MISMATCHES.md`
> and `PROCESS.md`.

> **Verified finding (radiology/lab/theatre):** neither the legacy nor the rewrite
> affiliated radiographers / lab techs / theatre staff to a facility. Legacy
> `Radiology`, `LabTest`, `Procedure` carried only an *optional ordering-clinician*
> FK; `Theatre` was a bare room entity. The rewrite already routes these by
> role-scoped worklist (`RADIOGRAPHER`, `LABORATORIST`) and binds procedures to a
> `theatreUid`. The only legacy facility↔staff M:Ns were Clinician⇄Clinic and
> StorePerson⇄Store — both now restored.

## Goal

Restore the legacy's **clinician ⇄ clinic affiliation** so that a consultation is
routed to a clinician who actually works at the chosen clinic — without cloning the
legacy's six parallel staff tables. Keep the proven *process* (per
[[process-fidelity]]); fix the *data-model* (enhanced clean-arch design).

The user's framing: "there is a relationship between user, clinician, radiologist,
clinic — look at the legacy and see how we can update."

---

## 1. What the legacy does (the proven process)

Legacy backend: `d:\My_Works\HMS\ZANAHMIS-2-feature\Zana-HMIS-API\...\domain\`

- **`User`** (auth: username, password, `code`, names, `@ManyToMany roles`).
- **Parallel clinical-staff entities**, each a near-clone with a nullable
  `@OneToOne` back to `User` and a shared `code`:
  `Clinician`, `Nurse`, `Pharmacist`, `Cashier`, `StorePerson`, `Management`.
  (There is **no** separate `Radiologist`/`LabTech`/`Doctor` — `Clinician.type`
  carries the specialization.)
- **`Clinician` ⇄ `Clinic`** is `@ManyToMany` (`Clinician.clinics`,
  inverse `Clinic.clinicians`) — a clinician works at one *or more* clinics.
- **`StorePerson` ⇄ `Store`** is `@ManyToMany` (the same pattern for inventory).
  Nurses/pharmacists/cashiers are **facility-wide** (no clinic link).
- Staff entities are **auto-created** when a `User` is saved with the matching role
  name (`UserServiceImpl`), and marked `active=false` when the role is removed.
  There is also an `assign_user_profile?id&code` endpoint to bind an existing User.
- **`Consultation`** has real FKs `clinic_id` **and** `clinician_id`; the booking UI
  picks a clinic, then a clinician **from that clinic's clinicians** ("a patient is
  sent to one clinic / one clinician … can be reassigned").

**Net process to preserve:** *clinicians are affiliated to specific clinics; booking
a consultation for a clinic only offers (and only accepts) clinicians of that clinic.*

---

## 2. What the rewrite does today (the gap)

Backend: `hmis-engine-api/.../com/otapp/hmis/engine/`

- **`iam`** owns `User` / `Role` / `Privilege` only. `User` has **no** clinic,
  specialty, license, or clinical-identity field. (`iam` module
  `allowedDependencies = {common}`.)
- **No** clinician/provider/staff entity exists. `hr.employee.Employee` is **HR/payroll
  only** — optional `username` link to a User, **no `Clinic` FK**, `designation`/
  `department` are free strings; not used for clinical routing.
- **`masterdata.clinic.Clinic`** ([Clinic.java](hmis-engine-api/src/main/java/com/otapp/hmis/engine/masterdata/clinic/domain/Clinic.java)) — `code`, `name`, `type` (`ClinicType`), `description`, `location`, `active`. **No staff reference of any kind.** (`masterdata` `allowedDependencies = {common, iam}`.)
- **`Consultation.clinicianUsername`** is a **free `String(64)` — no FK**, and
  [ConsultationService.book()](hmis-engine-api/src/main/java/com/otapp/hmis/engine/encounter/consultation/application/ConsultationService.java#L46) validates only that the user **exists and is enabled**. It does **not** check the user has the `CLINICIAN` role, nor that they belong to the clinic. (`encounter` `allowedDependencies = {common, iam, masterdata, patient}`.)
- **`StaffDirectoryService.findByRole(role)`** ([StaffDirectoryService.java](hmis-engine-api/src/main/java/com/otapp/hmis/engine/iam/application/StaffDirectoryService.java)) returns **all** enabled users with a role via `UserRepository.findEnabledByRoleName`, with **no clinic filter**. Endpoint `GET /iam/staff/by-role/{roleName}`.
- **Frontend** ([send-to-doctor-modal.component.ts](hmis-engine-web/src/app/features/encounter/consultation/send-to-doctor-modal.component.ts), [start-consultation.component.ts](hmis-engine-web/src/app/features/encounter/consultation/start-consultation.component.ts)) loads **all** active clinics and **all** `CLINICIAN` users into **independent** dropdowns — any clinician can be paired with any clinic.

### Gap inventory

| # | Legacy behavior | Current behavior | Disposition |
|---|---|---|---|
| R1 | Clinician affiliated to one/more clinics (`Clinician.clinics` M:N) | `ClinicClinician` (`md_clinic_clinician`) M:N affiliation | ✅ **Done (Phase 1)** |
| R2 | Booking offers only the clinic's clinicians | Dependent dropdowns load the clinic's clinicians | ✅ **Done (Phase 3)** |
| R3 | Booking stores clinician FK validated against the clinic | `book()`/`transfer()` assert role + clinic membership (hard gate) | ✅ **Done (Phase 2)** |
| R4 | Clinician auto-provisioned/affiliated via role | Affiliation managed on the Clinic "Clinicians" panel | ✅ **Done (Phase 1 + 3)** |
| R5 | `StorePerson` ⇄ `Store` M:N | `StoreStaff` (`md_store_staff`) M:N; `issueTO` gated by membership | ✅ **Done (Phase 4a)** |
| R6 | `Clinician.type` (specialization), `code` | `ProviderProfile` (`iam_provider_profile`): specialty/registration/licence | ✅ **Done (Phase 4b)** |
| — | Nurse/Pharmacist/Cashier facility-wide; lab/radiology/theatre role-scoped | same (role-scoped) | ✅ **No change — verified faithful** |

---

## 3. Design decisions (clean-arch, modulith-aware)

1. **Enhance, don't clone.** Do **not** recreate `Clinician`/`Nurse`/… tables. The
   `User` + `Role` *is* the staff identity (this is exactly the kind of data-model
   simplification [[process-fidelity]] sanctions, like `ServicePrice` replacing six
   `*InsurancePlan` tables). We add only the *relationship* the legacy relied on.

2. **The affiliation lives in `masterdata` (clinic module).** Module rules force this:
   `iam → {common}` (so `iam` cannot reference `Clinic`), but `masterdata → {common, iam}`
   (so the clinic module *can* validate against `iam`). The clinic "owns" the list of
   clinicians who work there — which also matches the legacy inverse `Clinic.clinicians`.

3. **Loose coupling by identity string**, consistent with the rest of the codebase
   (uids/usernames, no cross-module FKs). The affiliation row stores `clinicUid` +
   the clinician's `userUid` (canonical) with `username` denormalized (because
   `Consultation` routes by `clinicianUsername`). No DB FK across modules.

4. **Validate at two points:** at *assignment* time (the user must hold the
   `CLINICIAN` role) and at *booking* time (the clinician must be affiliated with the
   chosen clinic). Booking validation is the enforceable gate; assignment validation
   is UX/defense-in-depth.

5. **Keep radiology/lab/pharmacy/nursing role-scoped** (no per-clinic or per-staff
   assignment) — the legacy did the same; only `Clinician`↔`Clinic` (and
   `StorePerson`↔`Store`) were affiliated.

---

## 4. Phase 1 — Clinic ⇄ Clinician affiliation (backend, masterdata)

New sub-package `masterdata/clinic/` additions:

- **`domain/ClinicClinician.java`** — entity, table `md_clinic_clinician`,
  `AuditableEntity` (gives `uid`). Columns: `clinic_uid` (26), `user_uid` (26),
  `username` (64, denormalized), `active`. Unique constraint `(clinic_uid, user_uid)`.
- **`domain/ClinicClinicianRepository.java`** —
  `findByClinicUidAndActiveTrue(clinicUid)`,
  `existsByClinicUidAndUsernameAndActiveTrue(clinicUid, username)`,
  `findByUserUid(userUid)`, `findByClinicUidAndUserUid(...)`.
- **`application/ClinicStaffService.java`** —
  `assignClinician(clinicUid, userUid)` (idempotent; resolves the clinic; resolves
  the user via an **iam-published lookup** and **asserts the `CLINICIAN` role**;
  denormalizes username), `removeClinician(clinicUid, userUid)`,
  `listClinicians(clinicUid)` → `List<StaffOption>`,
  `isAssigned(clinicUid, username)` → boolean (consumed by `encounter`).
- **iam published API for the role check.** Add to `StaffDirectoryService` a method
  `Optional<StaffOption> findActiveByUsernameInRole(username, roleName)` (or
  `boolean isUserInRole(username, role)`), backed by a new `UserRepository` query.
  `masterdata` may call this (allowed dep). Keeps `iam` ignorant of `Clinic`.
- **`api/ClinicStaffController.java`** (gated `MASTERDATA_MANAGE`):
  - `GET    /masterdata/clinics/uid/{clinicUid}/clinicians` → assigned clinicians
  - `POST   /masterdata/clinics/uid/{clinicUid}/clinicians` `{ userUid }` → assign
  - `DELETE /masterdata/clinics/uid/{clinicUid}/clinicians/uid/{userUid}` → remove
- **Migration `V56__clinic_clinician.sql`** — create `md_clinic_clinician` with the
  unique index + an index on `user_uid`. **Backfill: none** (no prior data); but see §8.

> URI shape follows [[uri-convention]]; DTOs carry id+uid, URLs use uid ([[id-uid-exposure]]).

## 5. Phase 2 — Consultation booking fidelity (encounter)

- **`ConsultationService.book()`** ([:46](hmis-engine-api/src/main/java/com/otapp/hmis/engine/encounter/consultation/application/ConsultationService.java#L46)): after the existing exists+enabled check, add:
  1. assert the clinician holds the `CLINICIAN` role (via iam `StaffDirectoryService`);
  2. assert `clinicStaffService.isAssigned(clinicUid, clinicianUsername)` —
     else `BusinessRuleException("Clinician <x> is not assigned to clinic <y>")`.
  `encounter` may depend on both `iam` and `masterdata`, so this is a legal direct call.
- **`transfer(...)`**: when a consultation is transferred to another clinic, apply the
  same membership check for the receiving clinic+clinician (legacy allows reassignment
  but to a clinician of the new clinic).
- A **clinic-scoped clinician lookup** for the UI: reuse
  `GET /masterdata/clinics/uid/{clinicUid}/clinicians` (Phase 1) rather than the
  unscoped `GET /iam/staff/by-role/CLINICIAN`.

## 6. Phase 3 — Frontend

- **Dependent dropdowns** in [send-to-doctor-modal](hmis-engine-web/src/app/features/encounter/consultation/send-to-doctor-modal.component.ts) and [start-consultation](hmis-engine-web/src/app/features/encounter/consultation/start-consultation.component.ts): on clinic change, load that clinic's clinicians from the new endpoint (clear the clinician control first). Replace the unconditional `staffService.byRole('CLINICIAN')`.
- **New `core/directory` (or clinic feature) service method**:
  `clinicCliniciansService.list(clinicUid)` → `StaffOption[]`.
- **Clinic admin UI**: on the clinic detail/edit page, a "Clinicians" panel —
  list assigned clinicians, "Add clinician" (typeahead over `CLINICIAN`-role users via
  `StaffDirectoryService.byRole`), remove. (Mirrors the legacy clinician page's
  multi-clinic selector, inverted to the clinic side.)
- **User view**: show a read-only "Works at clinics" list for `CLINICIAN` users
  (optional, nice-to-have).

## 7. Phase 4 — Optional extensions (only if the user wants them)

- **`StorePerson` ⇄ `Store`** (R5): same pattern in the `store` module
  (`store_person` affiliation), gating store issue/transfer screens by membership.
- **Clinician attributes** (R6): add `specialty`/`type` and a `registrationNo`/license
  to the affiliation or a small `provider_profile` — legacy `Clinician` had `type`+`code`
  only, so this is minimal and optional. Defer unless requested.

---

## 8. Cross-cutting notes

- **Module boundaries** (must stay green via `ApplicationModules.verify()`):
  `iam` gains no new deps; `masterdata`→`iam` (already allowed) for the role check;
  `encounter`→`masterdata`+`iam` (already allowed) for booking validation. **No module
  may import `Clinic`/`ClinicClinician` types across a boundary — go through the
  published service + DTOs.**
- **Seeding for QA**: with the new gate, a CASH consultation can't be booked until at
  least one clinician is assigned to a clinic. Add a dev/seed step (or a Flyway seed)
  assigning the bootstrap/sample clinicians to the sample clinics, mirroring the
  existing `SendToDoctorFlowIT` setup, so the running app isn't dead-on-arrival.
- **Backfill / migration order**: new tables only; existing consultations keep their
  free-string `clinicianUsername` (no rewrite). The gate applies to *new* bookings.

## 9. Verification (when executed)

1. **Backend** `cd hmis-engine-api && mvn -o test` (Docker up). New ITs:
   - `ClinicStaffIT` — assign requires `CLINICIAN` role (non-clinician → 4xx);
     list returns only assigned+active; remove drops them; assign is idempotent.
   - `ConsultationClinicMembershipIT` — booking a clinic with an **unassigned**
     clinician → `BusinessRuleException`; assigning then booking → succeeds;
     transfer enforces membership on the receiving clinic.
   - Adjust `SendToDoctorFlowIT` to assign the clinician to the clinic first.
   - Run `ApplicationModules.verify()` (no `encounter→billing`, no illegal `iam` deps).
2. **Frontend** `cd hmis-engine-web && npm run build`.
3. **Manual e2e**: register OUTPATIENT → assign Dr. X to Clinic A → "Send to doctor"
   for Clinic A offers only Dr. X → book → appears in Dr. X's reception queue.
   Confirm Clinic B (no clinicians) offers none and booking is refused.
4. Tick R1–R6 in this file and update `PROCESS.md` coverage.

---

## 10. Decisions to confirm before executing

1. **Scope**: just **clinician ⇄ clinic** (Phases 1–3, recommended), or also
   **StorePerson ⇄ Store** + clinician specialty/license (Phase 4)?
2. **Manage affiliations from**: the **Clinic** page (recommended — matches where the
   data lives), the **User** page, or both?
3. **Enforcement strength**: hard gate at booking (recommended — legacy behavior), or
   warn-only initially to avoid disrupting the running QA data?
4. **Multi-clinic**: confirm a clinician may belong to **several** clinics (legacy
   M:N) — the plan assumes yes.

---

### Critical files (for execution)
Backend — new: `masterdata/clinic/domain/ClinicClinician.java`,
`…/domain/ClinicClinicianRepository.java`, `…/application/ClinicStaffService.java`,
`…/api/ClinicStaffController.java`, DTOs, `db/migration/V56__clinic_clinician.sql`.
Modify: [StaffDirectoryService.java](hmis-engine-api/src/main/java/com/otapp/hmis/engine/iam/application/StaffDirectoryService.java) + [UserRepository.java](hmis-engine-api/src/main/java/com/otapp/hmis/engine/iam/domain/UserRepository.java) (role-check query), [ConsultationService.java](hmis-engine-api/src/main/java/com/otapp/hmis/engine/encounter/consultation/application/ConsultationService.java) (book/transfer gate).
Frontend — new: clinic-clinicians service + clinic-detail "Clinicians" panel; modify [send-to-doctor-modal.component.ts](hmis-engine-web/src/app/features/encounter/consultation/send-to-doctor-modal.component.ts), [start-consultation.component.ts](hmis-engine-web/src/app/features/encounter/consultation/start-consultation.component.ts) for clinic→clinician dependent dropdowns.
