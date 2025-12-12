# Integration Tests

This document describes the integration tests for the snippet ingestion workflow.

## Overview

The integration test (`test_integration.py`) uses Testcontainers to spin up real OpenSearch and Temporal instances, then runs the complete workflow end-to-end and verifies the results.

Pytest is configured to auto-discover all test files matching `test_*.py` or `*_test.py` patterns in the pipeline directory.

## Test Flow

1. **Container Setup**: Starts OpenSearch and Temporal containers
2. **Worker Start**: Launches a Temporal worker to execute activities
3. **Workflow Execution**: Triggers the snippet ingestion workflow
4. **Verification**: 
   - Checks workflow completes successfully
   - Verifies document exists in OpenSearch
   - Validates all fields are present and correctly formatted
   - Tests chunk creation and embedding
   - Performs basic semantic search

## Prerequisites

### 1. Install Dependencies

```bash
cd pipeline
uv sync --all-groups
# or
pip install -e ".[dev]"
```

### 2. Set Environment Variables

Create a `.env` file in the `pipeline/` directory with:

```env
OPENAI_API_KEY=your-openai-api-key-here
```

The test will use testcontainers for OpenSearch and Temporal, so you don't need docker-compose running.

### 3. Docker

Ensure Docker is running on your machine, as Testcontainers needs it to spin up containers.

## Running Tests

### Run All Tests

Using pytest directly:
```bash
cd pipeline
pytest -v -s
```

Using the convenience script:
```bash
cd pipeline
./run_integration_tests.fish
```

Using just (recommended):
```bash
cd pipeline
just test
```

### Run Specific Test

```bash
# Test workflow only
pytest test_integration.py::test_snippet_ingestion_workflow -v -s

# Test OpenSearch connection only
pytest test_integration.py::test_opensearch_connection -v -s

# Test Temporal connection only
pytest test_integration.py::test_temporal_connection -v -s

# Run only integration tests
pytest -m integration -v -s

# Run only unit tests (when available)
pytest -m unit -v -s

# Skip slow tests
pytest -m "not slow" -v -s
```

### Run with More Verbosity

```bash
pytest -vv -s --log-cli-level=INFO
```

### Filter Tests by Marker

The tests use pytest markers for categorization:

```bash
# Run only integration tests
pytest -m integration -v -s
# Or with just
just test-integration

# Run only slow tests
pytest -m slow -v -s

# Skip slow tests (useful for quick checks)
pytest -m "not slow" -v
# Or with just
just test-fast

# Combine markers
pytest -m "integration and not slow" -v
```

### Using Just Commands

The `justfile` provides convenient commands:

```bash
# Run all tests
just test

# Run integration tests only
just test-integration

# Run tests, skip slow ones
just test-fast

# Run specific test file
just test-file test_integration.py

# Run tests with coverage report
just test-cov

# List all available tests (no execution)
just test-list
```

## Expected Output

```
==============================================================================
Starting Integration Test: Snippet Ingestion Workflow
==============================================================================

📝 Testing with snippet ID: test-integration-snippet-001
Code length: 455 characters

🚀 Starting workflow...
✅ Workflow started: test-workflow-test-integration-snippet-001

⏳ Waiting for workflow to complete...

✅ Workflow completed successfully!
Result: {'snippet_id': 'test-integration-snippet-001', 'chunks_count': 3, ...}

📊 Workflow Results:
  - Snippet ID: test-integration-snippet-001
  - Chunks created: 3
  - Summary length: 245 chars
  - OpenSearch result: created

🔍 Verifying document in OpenSearch...
✅ Document found in OpenSearch!

🔬 Verifying document structure...
  ✓ Field 'snippet_id' present
  ✓ Field 'code' present
  ✓ Field 'overall_summary' present
  ✓ Field 'overall_embedding' present
  ✓ Field 'chunks' present
  ✓ Field 'created_at' present
  ✓ Field 'updated_at' present

📦 Chunk Verification:
  - Total chunks: 3
  ✓ Chunk 0: 152 chars code, 89 chars summary
  ✓ Chunk 1: 145 chars code, 76 chars summary
  ✓ Chunk 2: 158 chars code, 95 chars summary

🔍 Testing semantic search...
  ✓ Document searchable (found 1 results)

==============================================================================
✅ ALL TESTS PASSED!
==============================================================================
```

## Test Configuration

The test uses the following configuration:

- **OpenSearch Image**: `opensearchproject/opensearch:3.3.0` (same as docker-compose)
- **Temporal Image**: `temporalio/temporal:1.5.1` (same as docker-compose)
- **Test Index**: `test_code_snippets`
- **Namespace**: `default`
- **Task Queue**: `test-snippet-ingestion-queue`
- **Workflow Timeout**: 180 seconds

## Troubleshooting

### Test Hangs or Times Out

1. Check Docker is running: `docker ps`
2. Check container logs:
   ```bash
   docker ps -a
   docker logs <container-id>
   ```
3. Increase timeout in test if needed (line with `timeout=180.0`)

### OpenAI API Errors

1. Verify `OPENAI_API_KEY` is set in `.env`
2. Check API quota/rate limits
3. Try with a simpler code sample

### Port Conflicts

Testcontainers automatically assigns random ports, so there should be no conflicts even if docker-compose is running.

### Container Cleanup

Testcontainers automatically cleans up containers after tests. If you see orphaned containers:

```bash
docker ps -a | grep testcontainers
docker rm -f <container-id>
```

## What Gets Tested

### Workflow Execution
- ✅ Workflow starts successfully
- ✅ Workflow completes without errors
- ✅ Result contains all expected fields

### Document Structure
- ✅ Document is indexed in OpenSearch
- ✅ All required fields are present
- ✅ Field types are correct (text, keyword, knn_vector, etc.)

### Overall Summary & Embedding
- ✅ Summary is generated and non-empty
- ✅ Embedding is 1024-dimensional (OpenAI text-embedding-3-large)

### Chunks
- ✅ Code is chunked semantically
- ✅ Each chunk has code, summary, and embedding
- ✅ Chunk embeddings are 1024-dimensional
- ✅ Chunk indices are sequential

### Search
- ✅ Document is searchable
- ✅ Text search works on summary field

## CI/CD Integration

To run in CI/CD pipeline:

```yaml
# Example GitHub Actions
- name: Run Integration Tests
  run: |
    cd pipeline
    pytest -v  # Auto-discovery mode
  env:
    OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}

# Or run only fast tests in CI
- name: Run Fast Tests
  run: |
    cd pipeline
    pytest -m "not slow" -v
  env:
    OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
```

## Performance

Expected test duration:
- Container startup: ~20-30 seconds
- Workflow execution: ~30-60 seconds (depends on OpenAI API)
- Verification: ~5 seconds
- **Total**: ~1-2 minutes

## Future Enhancements

- [ ] Add tests for workflow retry behavior
- [ ] Add tests for invalid input handling
- [ ] Add performance/load tests
- [ ] Add tests for different programming languages
- [ ] Mock OpenAI for faster tests
- [ ] Add tests for vector similarity search
