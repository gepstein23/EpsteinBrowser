# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EpsteinBrowser is a web application that scrapes, processes, and indexes the Epstein files from government FOIA releases, then provides a beautiful, queryable UI for exploring the data. Priorities: **scalability, performance, data accuracy**.

### Primary Data Source

DOJ Epstein Library: `justice.gov/epstein` (~3.5M pages across 12 data sets). The system includes a **release scanner** that checks every 15 minutes for new data set publications and alerts via SNS.

### Current Phase

**Phase 1** — Document ingestion, text extraction, full-text search, browsing UI, new release detection. See `docs/project-plan.md`.

### Resume Point (last updated: 2026-02-05)

**Milestone M1: Project Scaffolding — IN PROGRESS**

Completed:
- M1.1 partially done: `frontend/` scaffolded with React + Vite + TypeScript. Packages installed (TailwindCSS, Vitest, testing-library, ESLint, React Router, React Query, Axios). `vite.config.ts` configured with Tailwind + Vitest. `package.json` has test script. `index.html` updated. `main.tsx` wired with BrowserRouter + QueryClientProvider. `App.tsx` has route structure (Home, Search, Document, Datasets). `index.css` set to Tailwind import. `App.css` is still Vite boilerplate (delete it).

Still needed for M1.1:
- Create `src/components/Layout.tsx` (shell with nav bar)
- Create `src/pages/HomePage.tsx`, `SearchPage.tsx`, `DocumentPage.tsx`, `DatasetsPage.tsx` (stub pages)
- Create `src/api/client.ts` (Axios instance)
- Create `src/test/setup.ts` (Vitest setup with testing-library)
- Create `src/App.test.tsx` (smoke test)
- Delete `src/App.css` and `src/assets/react.svg`
- Create `amplify.yml` in repo root
- Verify: `npm run build`, `npm run test`, `npm run lint` all pass

Not started:
- M1.2: Backend (Spring Boot multi-module Gradle project)
- M1.3: Infrastructure (Terraform modules)
- M1.4: CI (GitHub Actions + Amplify)

After M1, proceed to M2–M8 per `docs/project-plan.md`.

Git state: on `develop` branch, uncommitted work in `frontend/`. PRs #1 and #2 merged to main.

## Architecture

```
frontend/          → React + Vite + TypeScript (deployed via AWS Amplify)
backend/           → Java 21 + Spring Boot (deployed on ECS Fargate)
infrastructure/    → Terraform IaC for all AWS resources (dev + staging environments)
docs/              → Architecture documentation, diagrams, design docs
```

### Evented Document Pipeline

S3 is the source of truth. Processing is orchestrated by Step Functions as a multi-stage pipeline.

1. **Ingestion Service** (Fargate) scrapes government source → stores raw files in S3 `raw/` → registers in Aurora
2. **S3 trigger** kicks off Step Functions state machine
3. **Extract Text Worker** (Fargate) → OCR/text extraction → S3 `text/`
4. **NER Worker** (Fargate) → entity extraction + canonicalization → Aurora entities + OpenSearch `mentions`
5. **Claims Worker** (Fargate) → claim extraction + scoring (Bedrock for LLM enrichment) → OpenSearch `claims` + Aurora
6. **Query API** (Fargate) serves search, aggregations, entity/claim queries → frontend
7. **Frontend** displays stats dashboards, entity rankings, claims explorer, full-text search

### AWS Services

| Service | Purpose |
|---------|---------|
| S3 | Source of truth: `raw/`, `text/`, `derived/` prefixes |
| OpenSearch | Search index: `documents`, `mentions`, `claims` indices |
| Aurora PostgreSQL | Structured metadata: doc registry, entities, claims review, lineage |
| ECS Fargate | API, ingestion service, and pipeline workers |
| Step Functions | Pipeline orchestration (retryable, observable) |
| SQS | Backpressure queues between pipeline stages + DLQs |
| Lambda | Lightweight glue (triggers, status updates, fan-out) |
| Bedrock | Offline LLM enrichment (claim extraction, classification) |
| Cognito | API authentication (user pool + authorizer) |
| Amplify | Frontend hosting, CI/CD from `frontend/` |
| ECR | Docker image registry for backend |
| VPC | Network isolation for all compute + data stores |

## Build & Run Commands

### Frontend (`frontend/`)
```bash
npm install              # install dependencies
npm run dev              # local dev server
npm run build            # production build
npm run test             # run all tests
npm run test -- --run <file>  # run a single test file
npm run lint             # lint check
```

### Backend (`backend/`)
```bash
./gradlew build          # compile + test
./gradlew test           # run all tests
./gradlew test --tests "com.epstein.SomeTest"  # single test
./gradlew bootRun        # run locally
./gradlew bootJar        # build deployable JAR
```

### Infrastructure (`infrastructure/`)

Two environments: **dev** and **staging**, each with its own Terraform workspace and resource prefix.

```
infrastructure/
  modules/           → Shared Terraform modules
  environments/
    dev/             → Dev environment config (prefix: epstein-dev-)
    staging/         → Staging environment config (prefix: epstein-staging-)
```

```bash
# Apply to dev
cd infrastructure/environments/dev
terraform init
terraform plan
terraform apply

# Apply to staging
cd infrastructure/environments/staging
terraform init
terraform plan
terraform apply
```

## Workflow

- All work happens on the **`develop`** branch — always name the working branch `develop`
- Amplify builds automatically on push to `develop` (preview/staging)
- PRs are raised from `develop` against `main`; merge after PM approval
- All commit messages must begin with `Authored by Genevieve's Intern, Claude: `
- Every change must build and pass tests before PR and right before merge
- Frontend and backend changes in the same logical task go in the same PR
- Amplify deploys frontend automatically from `frontend/` on merge to `main`
- **Be very diligent**: whenever the PM provides new context or instructions, always update this CLAUDE.md immediately

## Documentation Standards

- **Always** provide architecture documentation with Mermaid diagrams where possible
- **Document every process and design** — security, observability, monitoring, data flow, API design, scalability, disaster recovery, and all operational concerns
- Architecture docs live in `docs/`:
  - `docs/architecture.md` — full system design (pipeline, data layer, security, monitoring, etc.)
  - `docs/claims-evidence-linking.md` — how claims link to evidence documents with verifiable chains
  - `docs/project-plan.md` — phased project plan (Phase 1: ingestion, browsing, release detection)
- Update documentation when architecture changes

## Code Standards

- Frontend: TypeScript strict mode, Vitest for testing, component tests for all UI
- Backend: Java 21, Spring Boot conventions, JUnit 5 + Mockito for testing, all services have unit tests
- Infrastructure: Terraform modules per resource group, environment-specific configs in `environments/dev/` and `environments/staging/`, all resources prefixed by environment (`epstein-dev-`, `epstein-staging-`)
- All code must be fully unit tested before commit
