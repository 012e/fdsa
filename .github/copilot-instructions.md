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

**Python Workflow System**
- Python 3.12+
- Temporal SDK for workflow orchestration
- LangChain + OpenAI for LLM operations
- OpenSearch for vector search
- Kafka consumer for event handling

**Infrastructure**
- Docker Compose for local development
- Kafka (Apache Kafka 4.x)
- Temporal Server (1.5.1)
- OpenSearch (2.11.1) with vector search
- PostgreSQL (18.1)

### System Flow

```
User Creates Snippet → Java API → PostgreSQL
                          ↓
                     Kafka Event (snippet.created)
                          ↓
                   Python Kafka Consumer
                          ↓
                   Temporal Workflow Started
                          ↓
        [Summarize → Embed → Chunk → Summarize Chunks → Embed Chunks]
                          ↓
                    OpenSearch Index
                          ↓
                   Semantic Search Ready
```

## Project Structure

```
/src/main/java/huyphmnat/fdsa/
├── snippet/           # Snippet domain logic
│   ├── dtos/         # Data transfer objects, events
│   ├── exceptions/   # Domain exceptions
│   ├── interfaces/   # Public interfaces
│   └── internal/     # Implementation (entities, repositories, services)
├── graph/            # GraphQL controllers
├── shared/           # Shared configurations (Kafka, Temporal, etc.)
└── Application.java  # Spring Boot entry point

/workflow/
├── activities/       # Temporal activities (individual tasks)
│   ├── summarize_code.py      # LLM code summarization
│   ├── chunk_code.py          # Semantic code chunking
│   ├── summarize_chunks.py    # Chunk summarization
│   ├── embed_text.py          # OpenAI embeddings
│   └── index_opensearch.py    # OpenSearch indexing
├── workflows/        # Temporal workflows (orchestration)
│   └── snippet_ingestion.py  # Main ingestion workflow
├── config/           # Configuration modules
├── ml/              # ML utilities (LLM, embeddings)
├── consumer.py      # Kafka event consumer
├── worker.py        # Temporal worker
└── test_workflow.py # Testing utilities
```

## Key Concepts

### 1. Snippet Lifecycle

1. **Creation**: User creates snippet via GraphQL → Saved to PostgreSQL
2. **Event Publishing**: `SnippetCreatedEvent` published to Kafka topic `snippet.created`
3. **Workflow Trigger**: Kafka consumer picks up event → Starts Temporal workflow
4. **Processing**: Workflow executes activities sequentially
5. **Indexing**: Final result stored in OpenSearch with embeddings
6. **Search Ready**: Snippet is searchable via semantic/vector search

### 2. Temporal Workflow Pattern

**Workflow**: Orchestrates activities, handles retries, maintains state
**Activities**: Individual, idempotent tasks (summarize, embed, index, etc.)
**Worker**: Process that executes activities
**Client**: Triggers workflows (Kafka consumer)

### 3. Event-Driven Architecture

**Events**:
- `snippet.created` - New snippet created
- `snippet.updated` - Snippet modified (TODO)
- `snippet.deleted` - Snippet removed (TODO)

**Event Schema** (SnippetCreatedEvent):
```java
{
  "id": UUID,
  "code": String
}
```

### 4. OpenSearch Document Structure

```json
{
  "snippet_id": "uuid",
  "code": "original code",
  "overall_summary": "AI-generated summary",
  "overall_embedding": [1024 floats],
  "codeChunks": [
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

## Development Guidelines

### Java/Spring Boot

**Patterns**:
- Use DTOs for data transfer between layers
- Keep domain logic in service implementations under `internal/`
- Use `EventService` for publishing Kafka events
- Follow Spring conventions (dependency injection, configuration)

**Event Publishing**:
```java
@Service
public class MyService {
    private final EventService eventService;
    
    public void createSnippet(Snippet snippet) {
        // ... save logic ...
        eventService.publish("snippet.created", 
            SnippetCreatedEvent.builder()
                .id(snippet.getId())
                .code(snippet.getCode())
                .build()
        );
    }
}
```

**Configuration**:
- Database: `application.yaml` → `spring.datasource.*`
- Kafka: `KafkaConfiguration.java` + `application.yaml`
- Temporal: `WorkflowConfiguration.java` + `application.yaml`

### Python Workflow System

**Activity Pattern**:
```python
from temporalio import activity

@activity.defn
async def my_activity(param: str) -> str:
    """Activity description."""
    # Do work
    return result
```

**Workflow Pattern**:
```python
from temporalio import workflow

@workflow.defn
class MyWorkflow:
    @workflow.run
    async def run(self, param: str) -> dict:
        result = await workflow.execute_activity(
            my_activity,
            param,
            start_to_close_timeout=timedelta(seconds=60),
            retry_policy=RetryPolicy(maximum_attempts=3),
        )
        return {"result": result}
```

**Best Practices**:
- Activities should be **idempotent** (safe to retry)
- Use appropriate timeouts for LLM calls (60s+)
- Configure retries for transient failures
- Log important steps for observability
- Handle errors gracefully

### Adding New Features

**New Event Type**:
1. Create event DTO in Java (`snippet/dtos/`)
2. Publish event via `EventService`
3. Add topic to `KAFKA_TOPICS` in `config/kafka.py`
4. Update consumer to handle new event type
5. Create/modify workflow as needed

**New Activity**:
1. Create activity file in `workflow/activities/`
2. Decorate with `@activity.defn`
3. Add to workflow imports
4. Register in `worker.py`
5. Call from workflow with proper timeout/retry

**New Workflow**:
1. Create workflow file in `workflow/workflows/`
2. Decorate with `@workflow.defn`
3. Implement `@workflow.run` method
4. Register in `worker.py`
5. Trigger from consumer or test script

## Common Tasks

### Running Locally

```bash
# Start infrastructure
docker compose up -d

# Run Java service
./mvnw spring-boot:run

# Run Python worker (terminal 1)
cd workflow
python worker.py

# Run Kafka consumer (terminal 2)
cd workflow
python consumer.py

# Test workflow
cd workflow
python test_workflow.py
```

### Testing

**Java Tests**:
```bash
./mvnw test
# Or specific test
./mvnw test -Dtest=SnippetServiceTest
```

**Python Workflow Test**:
```bash
cd workflow
python test_workflow.py
```

### Debugging

**Temporal Workflows**:
- UI: http://localhost:8233
- View workflow execution history
- See activity inputs/outputs
- Check retry attempts

**Kafka Events**:
- UI: http://localhost:6969
- View topics and messages
- Check consumer groups

**OpenSearch Data**:
- Dashboards: http://localhost:5601
- Dev Tools for queries
- View indices and documents

## Environment Variables

**Java (application.yaml)**:
```yaml
DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD
KAFKA_BOOTSTRAP_SERVERS
TEMPORAL_URL, TEMPORAL_NAMESPACE
```

**Python (workflow/.env)**:
```env
OPENAI_API_KEY                    # Required
KAFKA_BOOTSTRAP_SERVERS
TEMPORAL_HOST, TEMPORAL_NAMESPACE, TEMPORAL_TASK_QUEUE
OPENSEARCH_HOST, OPENSEARCH_PORT
```

## Dependencies

### Java
- Spring Boot 3.x (web, data-jpa, graphql)
- PostgreSQL driver
- Kafka client (spring-kafka)
- Lombok (code generation)

### Python
- temporalio>=1.20.0 (workflow orchestration)
- langchain>=1.1.3 (LLM framework)
- langchain-openai>=1.1.1 (OpenAI integration)
- opensearch-py>=3.1.0 (OpenSearch client)
- kafka-python>=2.3.0 (Kafka consumer)
- semchunk>=3.2.5 (semantic chunking)
- tiktoken (tokenization)

## Important Files

**Configuration**:
- `docker.compose.yaml` - Infrastructure services
- `pom.xml` - Java dependencies
- `workflow/pyproject.toml` - Python dependencies
- `src/main/resources/application.yaml` - Spring config
- `workflow/.env` - Python environment config

**Core Java Classes**:
- `snippet/internal/services/SnippetServiceImpl.java` - Main service
- `snippet/internal/services/EventServiceImpl.java` - Event publishing
- `shared/KafkaConfiguration.java` - Kafka setup
- `graph/controllers/SnippetController.java` - GraphQL endpoint

**Core Python Modules**:
- `workflows/snippet_ingestion.py` - Main workflow
- `activities/*.py` - All activities
- `consumer.py` - Kafka event handler
- `worker.py` - Temporal worker

**Documentation**:
- `workflow/README.md` - Full workflow documentation
- `workflow/QUICKSTART.md` - Quick start guide
- `workflow/ARCHITECTURE.md` - Architecture details

## Troubleshooting

**Import errors (Python)**:
```bash
cd workflow
uv sync  # or pip install -e .
```

**Worker not picking up tasks**:
- Check task queue name matches
- Ensure worker is running
- Verify Temporal connection

**Kafka events not consumed**:
- Check Kafka is running
- Verify topic exists
- Check consumer group in Kafka UI

**OpenSearch indexing fails**:
- Verify OpenSearch is running: `curl http://localhost:9200`
- Check disk space
- Review activity logs

**LLM/Embedding failures**:
- Verify OPENAI_API_KEY is set
- Check API rate limits
- Review activity timeout settings

## Future Enhancements (TODO)

1. **Update/Delete Workflows**: Handle snippet updates and deletions
2. **Search API**: GraphQL/REST API for semantic search
3. **Multi-language Support**: Language detection and appropriate chunking
4. **Batch Processing**: Process multiple snippets in one workflow
5. **Caching**: Cache summaries and embeddings
6. **Metrics**: Prometheus metrics for monitoring
7. **AST-based Chunking**: Better code structure understanding

## Code Style

**Java**:
- Follow Spring Boot conventions
- Use Lombok for boilerplate reduction
- Package-by-feature structure
- Internal implementations in `internal/` packages

**Python**:
- Follow PEP 8
- Type hints for all functions
- Async/await for I/O operations
- Docstrings for all public functions
- Use `uv` for dependency management

## Security Notes

- Never commit API keys (use `.env` files)
- OpenSearch security disabled for development only
- Enable authentication for production deployments
- Use secrets management for production

## Resources

- [Temporal Python SDK](https://docs.temporal.io/docs/python)
- [OpenSearch Python Client](https://opensearch.org/docs/latest/clients/python/)
- [LangChain Documentation](https://python.langchain.com/)
- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring for GraphQL](https://spring.io/projects/spring-graphql)

---

**When working with this codebase, remember:**
- Events drive the workflow system
- Temporal provides reliability and observability
- OpenSearch enables semantic code search
- Activities should be idempotent
- Always test workflows before deploying
