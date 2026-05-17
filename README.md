# HMIS Engine

A clean-architecture rewrite of the legacy Zana-HMIS hospital management system.

## Repository layout

This is a temporary monorepo containing two applications. They will be split into
separate repositories once the rewrite stabilises.

```
HMSCLEAN/
├── hmis-engine-api/    Spring Boot 3 / Java 21 backend (modular monolith)
├── hmis-engine-web/    Angular 18 frontend (SPA)
├── docs/               Architecture decision records, API conventions
└── docker-compose.yml  Local Postgres for development
```

## Bounded contexts (backend modules)

| Module        | Responsibility                                                    |
| ------------- | ----------------------------------------------------------------- |
| `iam`         | Users, roles, privileges, authentication, authorization           |
| `masterdata`  | Clinics, wards, pharmacies, stores, catalogs, pricing, insurance  |
| `patient`     | Patient registration, demographics, visits                        |
| `encounter`   | Consultations, admissions, transfers, discharge, deceased records |
| `orders`      | Lab tests, radiology, procedures, prescriptions                   |
| `pharmacy`    | Dispensing, stock cards, batch tracking, pharmacy↔store transfers |
| `procurement` | Suppliers, local purchase orders, goods received notes            |
| `billing`     | Bills, invoices, payments, insurance claims                       |
| `hr`          | Employees, payroll, assets                                        |
| `reporting`   | Cross-context read models and dashboards                          |

## Quick start

### Prerequisites

- Java 21
- Maven 3.9+
- Node 20+ and npm 10+
- Docker Desktop (for Postgres)

### Run locally

```bash
# 1. Start Postgres
docker compose up -d

# 2. Start the API (port 8080)
cd hmis-engine-api
./mvnw spring-boot:run

# 3. Start the web app (port 4200)
cd hmis-engine-web
npm install
npm start
```

Default root credentials are seeded on first startup:
- Username: `root`
- Password: `ChangeMe!123` (must be changed on first login in production)

## Architecture

- **Backend**: Spring Boot 3, modular monolith with Spring Modulith,
  hexagonal layering per module (`domain` / `application` / `infrastructure` / `api`).
- **Frontend**: Angular 18 standalone components + signals, feature folders per
  backend module, lazy-loaded routes.
- **Persistence**: PostgreSQL 16, Flyway migrations, no `ddl-auto`.
- **Auth**: JWT (access + refresh), BCrypt password hashing, role+privilege model.
- **Multi-tenancy**: Single-tenant deployments today, designed so a multi-tenant
  retrofit is mechanical (see `common/TenantContext`).

See [docs/](docs/) for architecture decision records.
