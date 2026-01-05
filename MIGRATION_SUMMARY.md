# Pipeline Migration Summary - Temporal to Spring Boot

## Overview

Successfully migrated the Python/Temporal-based pipeline to a simplified Spring Boot event-driven architecture.

## What Was Changed

### ✅ Removed Components

1. **Temporal Dependencies** (removed from pom.xml)
   - `io.temporal:temporal-sdk`
   - `io.temporal:temporal-testing`

2. **Temporal Configuration** (deleted)
   - `WorkflowConfiguration.java` - Removed entirely
   - Temporal URL/namespace from `application.yaml`

3. **Python Pipeline** (can be archived/removed)
   - `pipeline/workflows/` - No longer needed
   - `pipeline/activities/` - Replaced by Java services
   - `pipeline/consumers/` - Replaced by Spring Kafka listeners
   - `pipeline/worker.py` - No longer needed

### ✅ Added Components

#### Dependencies (pom.xml)
```xml
<!-- OpenSearch Java Client (NEW API, not deprecated) -->
<dependency>
    <groupId>org.opensearch.client</groupId>
    <artifactId>opensearch-java</artifactId>
    <version>2.11.1</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents.core5</groupId>
    <artifactId>httpcore5</artifactId>
    <version>5.2.4</version>
</dependency>
<dependency>
    <groupId>org.apache.httpcomponents.client5</groupId>
    <artifactId>httpclient5</artifactId>
    <version>5.3.1</version>
</dependency>
```

#### New Java Services

**1. OpenAI Integration**
- `OpenAIService.java` - Handles LLM calls and embeddings
  - `summarizeCode()` - Overall code summarization
  - `summarizeChunk()` - Chunk summarization
  - `embedText()` - Single text embedding
  - `embedTexts()` - Batch embeddings

**2. Code Processing**
- `CodeChunkingService.java` - Semantic code chunking
  - Simple line-based chunking (can be enhanced with tokenizers)

**3. OpenSearch Integration**
- `OpenSearchIndexingService.java` - Indexes to OpenSearch
  - Uses **new OpenSearch Java Client** (not deprecated)
  - `indexSnippet()` - Index snippet with embeddings and chunks

**4. Orchestration Services**
- `SnippetIngestionService.java` - Main snippet processing workflow
  - Orchestrates: summarize → embed → chunk → summarize chunks → embed chunks → index
- `RepositoryIngestionService.java` - Repository file processing
  - BFS traversal of repository
  - Async processing of individual files

**5. Event Listeners**
- `IngestionEventListener.java` - Kafka event consumers
  - Listens to `snippet.created` topic
  - Listens to `repository.cloned` topic

**6. Configuration**
- `OpenSearchConfiguration.java` - OpenSearch client bean
- Updated `GeneralConfigurations.java` - Added `@EnableAsync` and `ObjectMapper` bean

#### Configuration (application.yaml)
```yaml
# OpenAI Configuration
openai:
  api:
    key: ${OPENAI_API_KEY}
  model: ${OPENAI_MODEL:gpt-4o-mini}
  embedding:
    model: ${OPENAI_EMBEDDING_MODEL:text-embedding-3-large}

# OpenSearch Configuration
opensearch:
  host: ${OPENSEARCH_HOST:localhost}
  port: ${OPENSEARCH_PORT:9200}
  scheme: ${OPENSEARCH_SCHEME:http}
```

## Architecture Changes

### Before (Python/Temporal)
```
Kafka Event → Python Consumer → Temporal Workflow Started
                                        ↓
                              Temporal Activities (Python)
                                        ↓
                              OpenSearch (Python client)
```

### After (Spring Boot)
```
Kafka Event → Spring Kafka Listener → Spring Service (@Async)
                                            ↓
                                  Java Services (direct calls)
                                            ↓
                                  OpenSearch (Java client)
```

## Key Differences

| Aspect | Before (Temporal) | After (Spring) |
|--------|------------------|----------------|
| **Orchestration** | Temporal workflows with built-in retries | Spring @Async with manual error handling |
| **Activities** | Temporal activities (Python) | Spring service methods (Java) |
| **Language** | Python | Java |
| **Dependencies** | LangChain, OpenAI Python SDK | RestClient for OpenAI, OpenSearch Java Client |
| **Retry Logic** | Temporal's automatic retries | Manual retry logic (can add @Retryable) |
| **Observability** | Temporal UI | Spring logs + metrics (can add tracing) |
| **Chunking** | semchunk + tiktoken | Simple line-based (can be enhanced) |

## Migration Path

### Python → Java Mapping

| Python Activity | Java Service/Method |
|----------------|---------------------|
| `summarize_code.py` | `OpenAIService.summarizeCode()` |
| `summarize_chunks.py` | `OpenAIService.summarizeChunk()` |
| `embed_text.py` | `OpenAIService.embedText/embedTexts()` |
| `chunk_code.py` | `CodeChunkingService.chunkCode()` |
| `index_opensearch.py` | `OpenSearchIndexingService.indexSnippet()` |
| `repository_actions.py` | `RepositoryIngestionService` uses existing `RepositoryFileService` |

| Python Workflow | Java Service |
|----------------|--------------|
| `SnippetIngestionWorkflow` | `SnippetIngestionService.ingestSnippet()` |
| `RepositoryIngestionWorkflow` | `RepositoryIngestionService.ingestRepository()` |

| Python Consumer | Java Listener |
|----------------|---------------|
| `consumers/ingest.py` | `IngestionEventListener` |

## Setup Instructions

### 1. Install Dependencies
```bash
./mvnw clean install
```

### 2. Set Environment Variables
```bash
export OPENAI_API_KEY="your-openai-api-key"
# Optional overrides:
export OPENAI_MODEL="gpt-4o-mini"
export OPENAI_EMBEDDING_MODEL="text-embedding-3-large"
export OPENSEARCH_HOST="localhost"
export OPENSEARCH_PORT="9200"
```

### 3. Start Infrastructure
```bash
docker compose up -d
```

### 4. Run Spring Boot Application
```bash
./mvnw spring-boot:run
```

### 5. Run Tests
```bash
# Run all tests
./mvnw test

# Run only integration tests
./mvnw test -Dtest=IngestionIntegrationTest

# Run only unit tests
./mvnw test -Dtest=CodeChunkingServiceTest,SnippetIngestionServiceTest
```

### 6. Test Ingestion

**Option A: Create a Snippet**
```graphql
mutation {
  createSnippet(input: {
    code: "def hello(): print('world')"
    language: "python"
  }) {
    id
  }
}
```

**Option B: Clone a Repository**
```graphql
mutation {
  cloneRepository(input: {
    sourceUrl: "https://github.com/user/repo"
    identifier: "user/repo"
  }) {
    id
  }
}
```

## What to Delete (Optional Cleanup)

You can safely delete the entire `pipeline/` directory:
```bash
rm -rf pipeline/
```

Or keep it for reference if needed.

## Testing

### Test Coverage

Comprehensive tests have been created using TestContainers:

**Integration Tests** (`IngestionIntegrationTest.java`):
- Full end-to-end testing with real Kafka, PostgreSQL, and OpenSearch containers
- Mocks OpenAI API using MockWebServer
- Tests complete workflow: Event → Processing → Indexing
- Includes:
  - Basic snippet ingestion test
  - Multiple chunks handling test
  - OpenAI API interaction verification

**Unit Tests**:
- `CodeChunkingServiceTest.java` - Tests code chunking logic
- `SnippetIngestionServiceTest.java` - Tests orchestration with mocked dependencies

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=IngestionIntegrationTest

# Run specific test method
./mvnw test -Dtest=IngestionIntegrationTest#testSnippetIngestion
```

### Test Dependencies Added

```xml
<!-- MockWebServer for mocking HTTP APIs -->
<dependency>
    <groupId>com.squareup.okhttp3</groupId>
    <artifactId>mockwebserver</artifactId>
    <version>4.12.0</version>
    <scope>test</scope>
</dependency>

<!-- Awaitility for async testing -->
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <version>4.2.2</version>
    <scope>test</scope>
</dependency>

<!-- AssertJ for fluent assertions -->
<dependency>
    <groupId>org.assertj</groupId>
    <artifactId>assertj-core</artifactId>
    <version>3.27.3</version>
    <scope>test</scope>
</dependency>
```

### Test Architecture

```
Integration Test Setup:
├── TestContainers
│   ├── PostgreSQL (18.1-alpine)
│   ├── Kafka (apache/kafka-native:4.1.1)
│   └── OpenSearch (opensearchproject/opensearch:2.11.1)
├── MockWebServer (OpenAI API mock)
└── Spring Boot Test Context

Test Flow:
Test → Kafka Event → Listener → Service → OpenAI (mocked) → OpenSearch
                                                                   ↓
                                                         Verification
```

## What to Delete (Optional Cleanup)

You can safely delete the entire `pipeline/` directory:
```bash
rm -rf pipeline/
```

Or keep it for reference if needed.

## Future Enhancements

1. **Better Chunking**: Integrate a Java tokenizer library (e.g., JTokkit for GPT tokenization)
2. **Retry Logic**: Add `@Retryable` annotation for automatic retries
3. **Circuit Breaker**: Add Resilience4j for circuit breaking on OpenAI calls
4. **Rate Limiting**: Implement rate limiting for OpenAI API calls
5. **Batch Processing**: Process multiple snippets in batches
6. **Observability**: Add distributed tracing (Sleuth/Micrometer)
7. **Search API**: Implement semantic search endpoints using OpenSearch vector search

## Notes

- **OpenSearch Client**: Using the **new Java Client** (`opensearch-java`), not the deprecated high-level REST client
- **Async Processing**: File processing runs asynchronously using Spring's `@Async`
- **Error Handling**: Errors are logged but don't block the main application
- **Chunking**: Current implementation is simple; consider enhancing for production
- **Testing**: You may want to add integration tests for the ingestion services

## Troubleshooting

### OpenAI API Errors
- Check `OPENAI_API_KEY` is set correctly
- Verify API rate limits
- Check network connectivity

### OpenSearch Connection Issues
- Ensure OpenSearch is running: `curl http://localhost:9200`
- Check `opensearch.host` and `opensearch.port` in config
- Verify network connectivity

### Kafka Events Not Consumed
- Check Kafka is running
- Verify topics exist: `snippet.created`, `repository.cloned`
- Check consumer group in Kafka UI (http://localhost:6969)

### Async Methods Not Working
- Ensure `@EnableAsync` is present in configuration
- Check logs for async exceptions

## Success Criteria

✅ Spring Boot application starts without Temporal dependencies
✅ Kafka listeners consume events from `snippet.created` and `repository.cloned`
✅ OpenAI API calls work for summarization and embeddings
✅ Code chunking produces reasonable chunks
✅ Documents are indexed to OpenSearch successfully
✅ Can query OpenSearch and find indexed snippets

## Documentation Updated

- ✅ pom.xml - Dependencies updated
- ✅ application.yaml - Configuration updated
- ✅ This migration summary created
- ⚠️ Consider updating README.md with new architecture
- ⚠️ Consider updating .github/copilot-instructions.md to remove Temporal references

---

**Migration completed successfully!** 🎉

The system is now fully Spring Boot-based with no external orchestration dependencies.

