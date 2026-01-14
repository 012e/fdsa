# Code Snippet Search Engine - Copilot Instructions

## Project Overview

This is a **code snippet search and management system** using OpenSearch for semantic code search. The system ingests code snippets, processes them with AI for summarization and semantic understanding, and stores them in OpenSearch with vector embeddings for similarity search.

## Architecture

### Technology Stack

**Backend (Java/Spring Boot)**
- Java 21+ with Spring Boot 3.x
- PostgreSQL for persistent storage
- Kafka for event streaming
- Temporal for workflow orchestration (via Python workers)
- GraphQL API for queries

# Code Snippet Search — Copilot Instructions

Purpose: help an AI coding agent become productive in this repo quickly. Focus on the workflow pipeline, run/debug commands, repo conventions, and key integration points.

Quick facts
- Java backend: `src/main/java/huyphmnat/fdsa` (Spring Boot app). Run with `./mvnw spring-boot:run`.
- Python workflow: `workflow/` contains Temporal workflows, activities, `worker.py` and `consumer.py`.
- Dev infra: `docker-compose.yaml` (Kafka, Temporal, OpenSearch, Postgres).

Where to start (fast path)
- Start infra: `docker compose up -d` from repo root.
- Start Java API: `./mvnw spring-boot:run` (checks `src/main/resources/application.yaml`).
- Start Python worker: `cd workflow && python worker.py` (worker registers activities in `worker.py`).
- Start consumer: `cd workflow && python consumer.py` (listens to Kafka `snippet.created`).

Important flows and files (examples)
- Snippet lifecycle: GraphQL → Postgres → Kafka `snippet.created` → `workflow/consumer.py` → Temporal workflow `workflow/workflows/snippet_ingestion.py` → activities in `workflow/activities/` → OpenSearch index (see `workflow/activities/index_opensearch.py`).
- Event schema example: `SnippetCreatedEvent` (id UUID, code String) — see Java DTOs under `snippet/dtos/`.
- Key Java services: `snippet/internal/services/SnippetServiceImpl.java`, `EventServiceImpl.java` (publishes Kafka events).

Project-specific conventions
- Java: package-by-feature; put domain impls in `internal/` packages and expose DTOs in `dtos/`.
- Python/Temporal: activities must be idempotent and registered in `worker.py`. Timeouts and retry policies are set when calling `workflow.execute_activity` in `workflow` files.
- Config: runtime secrets live in `workflow/.env` (Python) and `src/main/resources/application.yaml` (Java). Do not commit keys.

Run & debug tips
- Temporal UI: http://localhost:8233. OpenSearch Kibana: http://localhost:5601. Kafka UI (if enabled): http://localhost:6969.
- To reproduce ingestion locally: run infra, run Java API, create snippet via GraphQL (or API client), then watch `consumer.py` and `worker.py` logs for workflow execution.
- If activities are not executed: confirm Temporal namespace & task queue in `src/main/resources/application.yaml` and `workflow/worker.py` match.

Tests and dev tasks
- Java tests: `./mvnw test` or `./mvnw test -Dtest=SnippetServiceTest`.
- Python workflow tests: `cd workflow && python test_workflow.py`. Integration tests: `tests/integration/test_activities.py`.

Common pitfalls
- Missing `OPENAI_API_KEY` causes LLM/embedding failures. Check `workflow/.env`.
- Schema drift between Java DTOs and consumer payloads will break workflows — update both sides when changing the event.

If you need more detail
- Look at `workflow/README.md`, `workflow/QUICKSTART.md`, and `workflow/ARCHITECTURE.md` for deeper workflow docs.

If anything here is unclear or you want additional examples (e.g., a step-by-step ingestion run or sample payloads), tell me which part to expand.
├── shared/           # Shared configurations (Kafka, Temporal, etc.)
