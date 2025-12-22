# Snippet Ingestion Pipeline (TypeScript)

This is the TypeScript implementation of the snippet ingestion pipeline, migrated from Python. It uses Temporal for workflow orchestration, Kafka for event-driven triggering, and OpenSearch for vector search indexing.

## Architecture

```
Java API creates snippet → Kafka event (snippet.created/updated)
                              ↓
                      Kafka Consumer (@platformatic/kafka)
                              ↓
                      Triggers Temporal Workflow
                              ↓
                      Temporal Worker executes activities:
                      1. Summarize code (OpenAI/Vercel AI SDK)
                      2. Embed overall summary (OpenAI embeddings)
                      3. Chunk code (pseudo-implementation for now)
                      4. Summarize chunks (OpenAI)
                      5. Embed chunk summaries (OpenAI embeddings)
                      6. Index to OpenSearch
```

## Package Structure

```
ts/workers/
├── shared/          # Shared types, schemas, config
│   ├── src/
│   │   ├── events.ts      # Zod schemas for Kafka events
│   │   ├── config.ts      # Configuration with validation
│   │   ├── types.ts       # Shared TypeScript types
│   │   └── index.ts       # Package exports
│   └── package.json
├── runner/          # Temporal worker (executes workflows & activities)
│   ├── src/
│   │   ├── activities/    # All Temporal activities
│   │   │   ├── summarize-code.ts
│   │   │   ├── chunk-code.ts
│   │   │   ├── summarize-chunks.ts
│   │   │   ├── embed-text.ts
│   │   │   └── index-opensearch.ts
│   │   ├── workflows/     # Temporal workflows
│   │   │   └── snippet-ingestion.ts
│   │   └── worker.ts      # Worker entry point
│   └── package.json
└── consumer/        # Kafka consumer (triggers workflows)
    ├── src/
    │   └── index.ts       # Kafka consumer implementation
    └── package.json
```

## Prerequisites

- Node.js 22+ (or compatible version)
- pnpm package manager
- Temporal server running (localhost:7233)
- Kafka running (localhost:9092)
- OpenSearch running (localhost:9200)
- OpenAI API key

## Setup

### 1. Install Dependencies

From the `ts/` root directory:

```bash
pnpm install
```

This will install dependencies for all packages in the monorepo.

### 2. Configure Environment Variables

Copy the example environment file and fill in your configuration:

```bash
cd ts
cp .env.example .env
```

Edit `.env` and set your OpenAI API key:

```env
OPENAI_API_KEY=sk-your-actual-key-here
```

Other variables have sensible defaults for local development.

### 3. Start Infrastructure

Make sure these services are running:

```bash
# From project root
docker compose up -d
```

This starts:
- Kafka (localhost:9092)
- Temporal (localhost:7233, UI at localhost:8233)
- OpenSearch (localhost:9200)
- PostgreSQL (for the Java API)

## Running

### Start Everything

From the `ts/` root directory:

```bash
pnpm dev
```

This starts:
- **Frontend** (port 5173)
- **Kafka Consumer** (listens for snippet events)
- **Temporal Worker** (processes workflows)

All three processes run concurrently with color-coded output.

### Start Individual Components

If you want to run components separately:

```bash
# Consumer only
cd ts/workers/consumer
pnpm start

# Worker only
cd ts/workers/runner
pnpm start

# Frontend only
cd ts/frontend
pnpm dev
```

## Testing the Pipeline

### 1. Create a Snippet via GraphQL

Use the Java API (typically at http://localhost:8080/graphql):

```graphql
mutation {
  createSnippet(input: {
    owner: "test-user"
    path: "example.ts"
    code: "function hello() { console.log('Hello, world!'); }"
  }) {
    id
    owner
    path
    code
  }
}
```

### 2. Watch the Logs

The consumer and worker will log their activity:

```
[consumer] Received message from topic: snippet.created
[consumer] Processing snippet.created for snippet <uuid>
[consumer] Started workflow snippet-ingestion-<uuid>

[runner] Starting ingestion workflow for snippet <uuid>
[runner] Generating overall summary...
[runner] Generating embedding for overall summary...
[runner] Chunking code...
[runner] Created 1 chunks
[runner] Generating chunk summaries...
[runner] Generating embeddings for chunk summaries...
[runner] Indexing to OpenSearch...
[runner] Workflow completed successfully
```

### 3. Check Temporal UI

Open http://localhost:8233 to see workflow execution history, activity results, and any retries.

### 4. Query OpenSearch

Check that the snippet was indexed:

```bash
curl -X GET "http://localhost:9200/code_snippets/_search?pretty"
```

Or use OpenSearch Dashboards at http://localhost:5601.

## Development

### Type Checking

Check types without running:

```bash
# Check all packages
cd ts/workers/shared && pnpm typecheck
cd ts/workers/runner && pnpm typecheck
cd ts/workers/consumer && pnpm typecheck
```

### Adding New Activities

1. Create activity file in `runner/src/activities/`
2. Export from `runner/src/activities/index.ts`
3. Import and use in workflow (`runner/src/workflows/snippet-ingestion.ts`)

### Adding New Events

1. Add Zod schema to `shared/src/events.ts`
2. Update consumer to handle the new event in `consumer/src/index.ts`

## Configuration

All configuration is centralized in `shared/src/config.ts` and loaded from environment variables.

### Environment Variables

See `.env.example` for all available variables. Key ones:

- `OPENAI_API_KEY` - **Required** - Your OpenAI API key
- `OPENAI_MODEL` - LLM model for summarization (default: `gpt-4o-mini`)
- `OPENAI_EMBEDDING_MODEL` - Embedding model (default: `text-embedding-3-large`)
- `OPENAI_EMBEDDING_DIMENSIONS` - Embedding dimensions (default: `1024`)
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers (default: `localhost:9092`)
- `TEMPORAL_HOST` - Temporal server (default: `localhost:7233`)
- `OPENSEARCH_HOST` - OpenSearch host (default: `localhost`)

## Troubleshooting

### Consumer can't connect to Kafka

Check that Kafka is running:
```bash
docker ps | grep kafka
```

### Worker can't connect to Temporal

Check that Temporal is running:
```bash
docker ps | grep temporal
curl http://localhost:7233
```

### OpenSearch indexing fails

Check OpenSearch is running and accessible:
```bash
curl http://localhost:9200
```

### LLM/Embedding API errors

- Check `OPENAI_API_KEY` is set correctly in `.env`
- Check your OpenAI account has credits
- Check rate limits if seeing 429 errors

## TODO

- [ ] Implement proper semantic chunking (currently returns whole code as one chunk)
- [ ] Add support for `snippet.deleted` event
- [ ] Add tests for activities and workflows
- [ ] Add metrics and monitoring
- [ ] Add circuit breaker for OpenAI API calls
- [ ] Support different LLM providers (Anthropic, etc.)

## Comparison with Python Implementation

| Feature | Python | TypeScript |
|---------|--------|------------|
| LLM | LangChain + OpenAI | Vercel AI SDK + OpenAI |
| Embeddings | LangChain OpenAIEmbeddings | Vercel AI SDK embed/embedMany |
| Chunking | semchunk | **TODO** - pseudo-implementation |
| Kafka | kafka-python | @platformatic/kafka |
| Temporal | temporalio Python SDK | @temporalio/* packages |
| OpenSearch | opensearch-py | @opensearch-project/opensearch |
| Config | python-dotenv + Pydantic | dotenv + Zod |
| Execution | `python worker.py` / `python consumer.py` | `tsx` via `pnpm start` |
