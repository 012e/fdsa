# Ingestion Service Tests

This directory contains comprehensive tests for the code snippet ingestion pipeline.

## Test Structure

### Integration Tests

**`IngestionIntegrationTest.java`**
- Full end-to-end integration tests using TestContainers
- Tests the complete flow: Kafka event → Processing → OpenSearch indexing
- Uses:
  - TestContainers for OpenSearch (opensearchproject/opensearch:2.11.1)
  - TestContainers for Kafka (from BaseIntegrationTest)
  - TestContainers for PostgreSQL (from BaseIntegrationTest)
  - MockWebServer to mock OpenAI API responses
  - Awaitility for async assertion

**Test Cases:**
1. `testSnippetIngestion()` - Basic snippet ingestion with single chunk
2. `testSnippetIngestionWithMultipleChunks()` - Large code snippet producing multiple chunks
3. `testOpenAIServiceDirectly()` - Verify OpenAI API interactions

### Unit Tests

**`CodeChunkingServiceTest.java`**
- Tests for the code chunking logic
- Test cases:
  - Small code (single chunk)
  - Large code (multiple chunks)
  - Line break preservation
  - Empty code handling
  - Very long single lines

**`SnippetIngestionServiceTest.java`**
- Unit tests for the snippet ingestion orchestration
- Uses Mockito to mock dependencies
- Test cases:
  - Successful ingestion flow
  - Multiple chunks processing
  - Error handling

## Running the Tests

### Run All Tests
```bash
./mvnw test
```

### Run Only Integration Tests
```bash
./mvnw test -Dtest=IngestionIntegrationTest
```

### Run Only Unit Tests
```bash
./mvnw test -Dtest=CodeChunkingServiceTest,SnippetIngestionServiceTest
```

### Run Specific Test Method
```bash
./mvnw test -Dtest=IngestionIntegrationTest#testSnippetIngestion
```

## Test Dependencies

The tests use the following libraries:

- **JUnit 5** - Test framework
- **TestContainers** - Docker containers for integration testing
  - PostgreSQL container
  - Kafka container
  - OpenSearch container
- **MockWebServer (OkHttp)** - Mock HTTP server for OpenAI API
- **Awaitility** - Async testing and polling
- **AssertJ** - Fluent assertions
- **Mockito** - Mocking framework

## Test Configuration

### Integration Test Profile

The tests use the `integration-testing` Spring profile defined in:
`src/test/resources/application-integration-testing.yaml`

Key configurations:
- Kafka consumer auto-offset set to `earliest`
- JPA hibernate set to `create-drop` for clean state
- Mock OpenAI and OpenSearch URLs (overridden by TestContainers)

### Dynamic Properties

TestContainers dynamically configures:
- PostgreSQL connection (host, port, credentials)
- Kafka bootstrap servers
- OpenSearch connection (host, port)
- OpenAI API base URL (MockWebServer)

## Understanding the Tests

### Integration Test Flow

1. **Setup Phase**
   - TestContainers starts PostgreSQL, Kafka, and OpenSearch
   - MockWebServer starts to mock OpenAI API
   - Spring Boot application starts with test configuration

2. **Test Execution**
   ```
   Test → Publish Kafka Event → Kafka Listener Receives
                                        ↓
                                  SnippetIngestionService
                                        ↓
                            OpenAI API (mocked) ← → OpenSearch
   ```

3. **Assertion**
   - Use Awaitility to poll OpenSearch until document appears
   - Verify document structure and content
   - Verify OpenAI API calls were made

### Mocking OpenAI Responses

The `setupMockOpenAIResponses()` method queues mock responses:
- Chat completion responses (for summarization)
- Embedding responses (1024-dimensional vectors)

Responses alternate between chat and embeddings to match the workflow:
1. Summarize overall → Embed summary
2. Chunk code → Summarize each chunk → Embed chunks

## Common Issues

### Tests Timeout
- OpenSearch container may take time to start
- Increase timeout in `await()` statements
- Check Docker resources (CPU, memory)

### Connection Refused
- Ensure Docker is running
- Check TestContainers can pull images
- Verify no port conflicts

### Kafka Events Not Consumed
- Check Kafka container is healthy
- Verify topic auto-creation is enabled
- Check consumer group configuration

## Test Data

### Mock Embeddings
- 1024-dimensional vectors (matching text-embedding-3-large)
- Generated with random values in tests

### Sample Code Snippets
- Python "hello world" function
- Large code file with 100 functions

## Future Enhancements

- [ ] Add tests for repository ingestion
- [ ] Add tests for error scenarios (API failures, network issues)
- [ ] Add performance tests for large repositories
- [ ] Add tests for concurrent ingestion
- [ ] Add tests for OpenSearch query/search functionality
- [ ] Mock LLM responses more realistically with different summaries
- [ ] Add tests for different programming languages

