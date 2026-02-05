# EpsteinBrowser — Architecture Documentation

## Table of Contents

- [System Overview](#system-overview)
- [High-Level Architecture](#high-level-architecture)
- [Data Flow](#data-flow)
- [Infrastructure & Environments](#infrastructure--environments)
- [Security](#security)
- [Observability & Monitoring](#observability--monitoring)
- [Network Architecture](#network-architecture)
- [Data Model](#data-model)
- [API Design](#api-design)
- [Scalability](#scalability)
- [Disaster Recovery](#disaster-recovery)

---

## System Overview

EpsteinBrowser is a public-facing web application that scrapes, processes, and indexes the Epstein files from government FOIA releases. It provides a beautiful, queryable UI for exploring the data with full-text search, aggregations, and statistical dashboards.

**Priorities**: Scalability, Performance, Data Accuracy.

---

## High-Level Architecture

```mermaid
graph TB
    subgraph "Users"
        U[Browser / Client]
    end

    subgraph "Frontend — AWS Amplify"
        FE[React + Vite + TypeScript<br/>SPA hosted on Amplify]
    end

    subgraph "Backend — ECS Fargate"
        API[Spring Boot REST API<br/>Fargate Service]
        SCRAPER[Scraper / Processor<br/>Fargate Task]
    end

    subgraph "Data Layer"
        S3[(S3<br/>Raw Documents)]
        OS[(OpenSearch<br/>Search Index)]
    end

    subgraph "Supporting Services"
        ECR[ECR<br/>Container Registry]
        CW[CloudWatch<br/>Logs & Metrics]
        ALB[Application<br/>Load Balancer]
    end

    U -->|HTTPS| FE
    FE -->|REST API calls| ALB
    ALB --> API
    API --> OS
    API --> S3
    SCRAPER -->|Fetch docs| GOV[Government<br/>FOIA Source]
    SCRAPER -->|Store raw files| S3
    SCRAPER -->|Index structured data| OS
    API -.->|Logs & Metrics| CW
    SCRAPER -.->|Logs & Metrics| CW
```

---

## Data Flow

```mermaid
sequenceDiagram
    participant GOV as Government Source
    participant SCRAPER as Scraper (Fargate)
    participant S3 as S3 Bucket
    participant PROC as Processor (Fargate)
    participant OS as OpenSearch
    participant API as API (Fargate)
    participant FE as Frontend (Amplify)
    participant USER as User

    Note over SCRAPER: Scheduled / On-demand
    SCRAPER->>GOV: Fetch FOIA document list
    GOV-->>SCRAPER: Document URLs / PDFs
    SCRAPER->>S3: Store raw documents (PDFs, images)
    SCRAPER->>PROC: Trigger processing
    PROC->>S3: Read raw document
    PROC->>PROC: Parse / OCR / Extract text & metadata
    PROC->>OS: Index structured document data

    Note over USER: User searches
    USER->>FE: Search query / browse stats
    FE->>API: REST API request
    API->>OS: OpenSearch query
    OS-->>API: Search results / aggregations
    API->>S3: Generate presigned URL (if viewing raw doc)
    API-->>FE: JSON response
    FE-->>USER: Rendered results
```

### Processing Pipeline Detail

```mermaid
flowchart LR
    A[Raw PDF/Image<br/>from S3] --> B[Text Extraction<br/>Apache Tika / OCR]
    B --> C[Metadata Extraction<br/>Names, Dates, Locations]
    C --> D[Data Validation<br/>& Deduplication]
    D --> E[Index to<br/>OpenSearch]
    D --> F[Store processed<br/>metadata to S3]
```

---

## Infrastructure & Environments

Two environments — **dev** and **staging** — with identical resource topology, differentiated by prefix.

```mermaid
graph TB
    subgraph "infrastructure/"
        subgraph "modules/"
            M1[VPC Module]
            M2[ECS Module]
            M3[OpenSearch Module]
            M4[S3 Module]
            M5[ALB Module]
            M6[Monitoring Module]
        end
        subgraph "environments/dev/"
            DEV[main.tf<br/>prefix: epstein-dev-]
        end
        subgraph "environments/staging/"
            STG[main.tf<br/>prefix: epstein-staging-]
        end
    end

    DEV --> M1 & M2 & M3 & M4 & M5 & M6
    STG --> M1 & M2 & M3 & M4 & M5 & M6
```

| Environment | Resource Prefix | Purpose |
|-------------|-----------------|---------|
| **dev** | `epstein-dev-` | Development and local testing |
| **staging** | `epstein-staging-` | Pre-production validation |

---

## Security

### Architecture

```mermaid
graph TB
    subgraph "Public"
        USER[User] -->|HTTPS only| CF[CloudFront / Amplify CDN]
        CF --> FE[Frontend SPA]
    end

    subgraph "DMZ — Public Subnet"
        ALB[ALB<br/>TLS termination<br/>WAF attached]
    end

    subgraph "Private Subnet"
        API[API Service<br/>Fargate]
        SCRAPER[Scraper<br/>Fargate]
    end

    subgraph "Isolated Subnet"
        OS[(OpenSearch<br/>VPC endpoint)]
        S3[(S3<br/>VPC endpoint)]
    end

    FE -->|HTTPS| ALB
    ALB --> API
    API --> OS
    API --> S3
    SCRAPER --> S3
    SCRAPER --> OS
```

### Security Controls

| Layer | Control | Details |
|-------|---------|---------|
| **Network** | VPC isolation | Fargate and OpenSearch in private subnets; no public IPs on compute |
| **Network** | Security groups | Least-privilege ingress/egress per service |
| **Network** | VPC endpoints | S3 and OpenSearch accessed via VPC endpoints, no internet transit |
| **Edge** | WAF | AWS WAF on ALB — rate limiting, OWASP top-10 rule set |
| **Edge** | TLS | TLS 1.2+ enforced on ALB and Amplify CDN |
| **Auth** | API authentication | API keys or Cognito tokens for frontend-to-API calls |
| **Data** | Encryption at rest | S3 SSE-S3, OpenSearch node-to-node encryption, EBS encryption |
| **Data** | Encryption in transit | All inter-service communication over TLS |
| **IAM** | Least privilege | Task roles per Fargate service with minimal permissions |
| **IAM** | No long-lived credentials | Fargate uses IAM task roles, no hardcoded keys |
| **Secrets** | AWS Secrets Manager | API keys, external credentials stored in Secrets Manager |
| **Supply chain** | ECR image scanning | Vulnerability scanning on push |
| **Logging** | Audit trail | CloudTrail enabled, all API calls logged |

---

## Observability & Monitoring

### Observability Stack

```mermaid
graph LR
    subgraph "Sources"
        API[API Logs]
        SCRAPER[Scraper Logs]
        ALB_LOGS[ALB Access Logs]
        OS_LOGS[OpenSearch Logs]
    end

    subgraph "Collection"
        CW[CloudWatch Logs]
        CWM[CloudWatch Metrics]
    end

    subgraph "Alerting"
        ALARM[CloudWatch Alarms]
        SNS[SNS Notifications]
    end

    subgraph "Dashboards"
        DASH[CloudWatch Dashboards]
    end

    API --> CW
    SCRAPER --> CW
    ALB_LOGS --> CW
    OS_LOGS --> CW

    API --> CWM
    SCRAPER --> CWM

    CWM --> ALARM
    CW --> ALARM
    ALARM --> SNS
    CWM --> DASH
    CW --> DASH
```

### Key Metrics & Alarms

| Metric | Source | Alarm Threshold | Action |
|--------|--------|----------------|--------|
| API response time (p99) | ALB target group | > 2s for 5 min | SNS alert |
| API error rate (5xx) | ALB | > 5% for 3 min | SNS alert |
| Fargate CPU utilization | ECS | > 80% for 10 min | Auto-scale + SNS alert |
| Fargate memory utilization | ECS | > 80% for 10 min | Auto-scale + SNS alert |
| OpenSearch cluster health | OpenSearch | RED for 1 min | SNS alert (critical) |
| OpenSearch free storage | OpenSearch | < 20% | SNS alert |
| OpenSearch JVM memory pressure | OpenSearch | > 80% for 15 min | SNS alert |
| Scraper task failures | ECS task status | Any failure | SNS alert |
| S3 bucket size | S3 | Informational | Dashboard only |
| 4xx error rate | ALB | > 10% for 5 min | SNS alert |

### Logging Strategy

| Service | Log Group | Retention | Format |
|---------|-----------|-----------|--------|
| API | `/ecs/epstein-{env}-api` | 30 days | JSON structured |
| Scraper | `/ecs/epstein-{env}-scraper` | 30 days | JSON structured |
| ALB | `epstein-{env}-alb-access-logs` (S3) | 90 days | ALB standard |
| OpenSearch | `/aws/opensearch/epstein-{env}` | 14 days | OpenSearch standard |

### Structured Log Format

All application logs use JSON structured logging:

```json
{
  "timestamp": "ISO-8601",
  "level": "INFO|WARN|ERROR",
  "service": "api|scraper|processor",
  "traceId": "uuid",
  "message": "description",
  "metadata": {}
}
```

### Distributed Tracing

- AWS X-Ray integrated with Spring Boot via `aws-xray-sdk`
- Trace IDs propagated across API → OpenSearch and API → S3 calls
- X-Ray service map provides visual dependency graph

---

## Network Architecture

```mermaid
graph TB
    subgraph "VPC: epstein-{env}-vpc (10.0.0.0/16)"
        subgraph "Public Subnets (10.0.1.0/24, 10.0.2.0/24)"
            ALB[Application Load Balancer]
            NAT[NAT Gateway]
        end
        subgraph "Private Subnets (10.0.10.0/24, 10.0.11.0/24)"
            API[API Fargate Tasks]
            SCRAPER[Scraper Fargate Tasks]
        end
        subgraph "Isolated Subnets (10.0.20.0/24, 10.0.21.0/24)"
            OS[OpenSearch Domain]
        end
        S3EP[S3 VPC Endpoint]
        OSEP[OpenSearch VPC Endpoint]
    end

    INET[Internet] --> ALB
    API --> NAT --> INET
    SCRAPER --> NAT
    API --> S3EP --> S3[(S3)]
    API --> OS
    SCRAPER --> S3EP
    SCRAPER --> OS
```

- **Multi-AZ**: All subnets span 2 AZs for high availability
- **NAT Gateway**: Outbound internet for Fargate tasks (scraper needs to reach government sources)
- **No public IPs** on any Fargate task or OpenSearch node

---

## Data Model

### OpenSearch Index Schema

```mermaid
erDiagram
    DOCUMENT {
        string document_id PK
        string source_url
        string s3_key
        string title
        string document_type
        date document_date
        date indexed_at
        text full_text
        string[] mentioned_names
        string[] mentioned_locations
        string[] mentioned_organizations
        int page_count
        string file_hash
    }

    NAME {
        string name_id PK
        string full_name
        string[] aliases
        int mention_count
        string[] document_ids
    }

    LOCATION {
        string location_id PK
        string name
        string type
        float lat
        float lon
        int mention_count
        string[] document_ids
    }

    DOCUMENT ||--o{ NAME : mentions
    DOCUMENT ||--o{ LOCATION : references
```

---

## API Design

### Endpoints

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/api/v1/search` | Full-text search with filters, pagination |
| `GET` | `/api/v1/documents/{id}` | Get single document metadata |
| `GET` | `/api/v1/documents/{id}/content` | Get presigned S3 URL for raw document |
| `GET` | `/api/v1/stats/overview` | Aggregate stats (total docs, names, locations) |
| `GET` | `/api/v1/stats/names` | Top mentioned names with counts |
| `GET` | `/api/v1/stats/locations` | Top mentioned locations with counts |
| `GET` | `/api/v1/stats/timeline` | Document count by date |
| `GET` | `/api/v1/names/{name}` | Documents mentioning a specific name |
| `GET` | `/api/v1/health` | Health check |

---

## Scalability

```mermaid
graph LR
    subgraph "Horizontal Scaling"
        A[ALB] --> B1[API Task 1]
        A --> B2[API Task 2]
        A --> B3[API Task N]
    end

    subgraph "OpenSearch Scaling"
        OS1[Data Node 1]
        OS2[Data Node 2]
        OS3[Data Node N]
    end

    B1 & B2 & B3 --> OS1 & OS2 & OS3
```

| Component | Scaling Strategy |
|-----------|-----------------|
| **Frontend** | Amplify CDN — globally distributed, auto-scales |
| **API** | ECS auto-scaling on CPU/memory; min 2 tasks, max configurable |
| **Scraper** | Run as on-demand Fargate tasks; parallelizable per document batch |
| **OpenSearch** | Horizontal: add data nodes. Vertical: increase instance size. Index sharding for parallelism |
| **S3** | Effectively unlimited; no scaling needed |

---

## Disaster Recovery

| Component | RPO | RTO | Strategy |
|-----------|-----|-----|----------|
| **S3 (raw docs)** | 0 | Minutes | S3 versioning + cross-region replication |
| **OpenSearch** | < 1 hour | < 1 hour | Automated snapshots to S3, restore to new domain |
| **Fargate** | N/A | Minutes | Stateless; redeploy from ECR image |
| **Frontend** | N/A | Minutes | Redeploy from git via Amplify |
| **Infrastructure** | N/A | < 1 hour | Terraform state in S3 + DynamoDB lock; re-apply |
