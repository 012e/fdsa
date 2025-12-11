# Snippet Ingestion Workflow

This workflow system automatically processes code snippets for search engine ingestion using Temporal, Kafka, and OpenSearch.

## Overview

When a `snippet.created` event is published to Kafka, the system:
1. Generates an overall summary of the code using LangChain + OpenAI
2. Creates embeddings for the summary
3. Chunks the code semantically using semchunk
4. Generates summaries for each chunk
5. Creates embeddings for all chunk summaries
6. Indexes everything to OpenSearch with vector search capabilities

## Architecture

- **Kafka**: Event streaming for snippet creation events
- **Temporal**: Workflow orchestration and reliability
- **OpenSearch**: Vector database for semantic code search
- **LangChain + OpenAI**: LLM for summarization and embeddings

## Prerequisites

1. Python 3.12+
2. Docker and Docker Compose (for services)
3. OpenAI API key

## Installation

1. Install dependencies:
```bash
cd workflow
uv sync
# or
pip install -e .
```

2. Create a `.env` file in the `workflow` directory:
```env
OPENAI_API_KEY=your_openai_api_key_here

# Optional configurations
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KAFKA_GROUP_ID=snippet-ingestion-worker
TEMPORAL_HOST=localhost:7233
TEMPORAL_NAMESPACE=default
TEMPORAL_TASK_QUEUE=snippet-ingestion-queue
OPENSEARCH_HOST=localhost
OPENSEARCH_PORT=9200
```

## Running the Services

1. Start infrastructure services (from project root):
```bash
docker compose up -d
```

This starts:
- Postgres (port 5432)
- Kafka (port 9092)
- Temporal (ports 7233, 8233)
- Kafka UI (port 6969)
- OpenSearch (port 9200)
- OpenSearch Dashboards (port 5601)

2. Start the Temporal worker (processes activities):
```bash
cd workflow
python worker.py
```

3. Start the Kafka consumer (triggers workflows):
```bash
cd workflow
python consumer.py
```

## How It Works

### Event Flow

```
Java Service → Kafka (snippet.created) → Consumer → Temporal Workflow → Activities → OpenSearch
```

### Workflow Steps

1. **Summarize Code** (`summarize_code` activity)
   - Uses GPT-4 to generate a high-level summary
   - Timeout: 60s, Retries: 3

2. **Embed Summary** (`embed_text` activity)
   - Creates 1024-dim vector embedding using OpenAI
   - Timeout: 30s, Retries: 3

3. **Chunk Code** (`chunk_code` activity)
   - Semantically chunks code into ~512 token pieces
   - Uses tiktoken tokenizer
   - Timeout: 30s

4. **Summarize Chunks** (`summarize_chunks` activity)
   - Generates summary for each chunk
   - Timeout: 120s, Retries: 3

5. **Embed Chunks** (`embed_texts` activity)
   - Batch generates embeddings for all chunks
   - Timeout: 60s, Retries: 3

6. **Index to OpenSearch** (`index_snippet_to_opensearch` activity)
   - Creates index with kNN vector support
   - Stores code, summaries, and embeddings
   - Timeout: 60s, Retries: 3

### OpenSearch Document Structure

```json
{
  "snippet_id": "uuid",
  "code": "original source code",
  "overall_summary": "high-level summary",
  "overall_embedding": [1024 floats],
  "chunks": [
    {
      "chunk_index": 0,
      "code": "chunk text",
      "summary": "chunk summary",
      "embedding": [1024 floats]
    }
  ],
  "created_at": "timestamp",
  "updated_at": "timestamp"
}
```

## Monitoring

- **Temporal UI**: http://localhost:8233 - Monitor workflow executions
- **Kafka UI**: http://localhost:6969 - View Kafka topics and messages
- **OpenSearch Dashboards**: http://localhost:5601 - Query and visualize search data

## Testing

Test the workflow by creating a snippet through your Java service, which will publish a `snippet.created` event to Kafka:

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "code": "def hello():\n    print('Hello, World!')"
}
```

## Troubleshooting

### Worker not picking up activities
- Ensure the task queue name matches in all configurations
- Check Temporal UI for workflow status
- Verify worker is connected: check worker logs

### Kafka messages not being consumed
- Verify Kafka is running: `docker ps`
- Check topic exists in Kafka UI
- Verify bootstrap servers configuration

### OpenSearch indexing failures
- Check OpenSearch is running: `curl http://localhost:9200`
- Verify index creation in OpenSearch Dashboards
- Check for sufficient disk space

### Import errors
- Ensure all dependencies are installed: `uv sync`
- Check Python version: `python --version` (should be 3.12+)
- Verify virtual environment is activated

## Configuration

All configuration is done via environment variables. See `.env` file for options.

## Development

- Activities are in `activities/`
- Workflows are in `workflows/`
- Configuration is in `config/`
- ML models (LLM, embeddings) are in `ml/`

## License

[Your License]
