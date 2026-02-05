# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EpsteinBrowser is a web application that scrapes, processes, and indexes the Epstein files from government FOIA releases, then provides a beautiful, queryable UI for exploring the data. Priorities: **scalability, performance, data accuracy**.

## Architecture

```
frontend/          → React + Vite + TypeScript (deployed via AWS Amplify)
backend/           → Java 21 + Spring Boot (deployed on ECS Fargate)
infrastructure/    → Terraform IaC for all AWS resources (dev + staging environments)
docs/              → Architecture documentation, diagrams, design docs
```

### Data Flow

1. **Scraper** (Spring Boot batch job on Fargate) pulls documents from government source → stores raw files in **S3**
2. **Processor** parses/extracts structured data → indexes into **Amazon OpenSearch**
3. **API** (Spring Boot REST on Fargate) serves queries from OpenSearch → frontend
4. **Frontend** displays stats dashboards and exposes full-text search/querying

### AWS Services

| Service | Purpose |
|---------|---------|
| S3 | Raw document storage (PDFs, images) |
| OpenSearch | Full-text search index, aggregations for stats |
| ECS Fargate | Hosts API and scraper/processor containers |
| Amplify | Frontend hosting, CI/CD from `frontend/` |
| ECR | Docker image registry for backend |
| VPC | Network isolation for Fargate + OpenSearch |

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
- Architecture docs live in `docs/` — see `docs/architecture.md` for the full system design
- Update documentation when architecture changes

## Code Standards

- Frontend: TypeScript strict mode, Vitest for testing, component tests for all UI
- Backend: Java 21, Spring Boot conventions, JUnit 5 + Mockito for testing, all services have unit tests
- Infrastructure: Terraform modules per resource group, environment-specific configs in `environments/dev/` and `environments/staging/`, all resources prefixed by environment (`epstein-dev-`, `epstein-staging-`)
- All code must be fully unit tested before commit
