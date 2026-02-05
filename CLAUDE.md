# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

EpsteinBrowser is a web application that scrapes, processes, and indexes the Epstein files from government FOIA releases, then provides a beautiful, queryable UI for exploring the data. Priorities: **scalability, performance, data accuracy**.

## Architecture

```
frontend/          → React + Vite + TypeScript (deployed via AWS Amplify)
backend/           → Java 21 + Spring Boot (deployed on ECS Fargate)
terraform/         → Terraform IaC for all AWS resources
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

### Terraform (`terraform/`)
```bash
terraform init           # initialize providers
terraform plan           # preview changes
terraform apply          # deploy infrastructure
```

## Workflow

- All work happens on the **`develop`** branch — always name the working branch `develop`
- Amplify builds automatically on push to `develop` (preview/staging)
- PRs are raised from `develop` against `main`; merge after PM approval
- All commit messages must begin with `Authored by Genevieve's Intern, Claude: `
- Every change must build and pass tests before PR
- Frontend and backend changes in the same logical task go in the same PR
- Amplify deploys frontend automatically from `frontend/` on merge to `main`
- Whenever the PM provides new context or instructions, update this CLAUDE.md accordingly

## Code Standards

- Frontend: TypeScript strict mode, Vitest for testing, component tests for all UI
- Backend: Java 21, Spring Boot conventions, JUnit 5 + Mockito for testing, all services have unit tests
- Terraform: modules per resource group, variables for all environment-specific values
- All code must be fully unit tested before commit
