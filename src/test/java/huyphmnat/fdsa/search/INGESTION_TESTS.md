# Ingestion Pipeline Test Suite

This document describes the comprehensive test suite created for the repository ingestion pipeline.

## Test Files Created

### 1. LanguageDetectionServiceTest
**Location**: `src/test/java/huyphmnat/fdsa/search/LanguageDetectionServiceTest.java`
**Type**: Unit Test
**Coverage**: `LanguageDetectionService`

**Test Cases**:
- `testDetectLanguage_ShouldReturnCorrectLanguage` - Parameterized test covering 20+ language/extension mappings
- `testDetectLanguage_NullFileName_ShouldReturnUnknown` - Null input handling
- `testDetectLanguage_EmptyFileName_ShouldReturnUnknown` - Empty string handling
- `testDetectLanguage_NoExtension_ShouldReturnUnknown` - Files without extensions (e.g., Makefile)
- `testDetectLanguage_DotFileWithoutExtension_ShouldReturnUnknown` - Dot files (e.g., .gitignore)
- `testDetectLanguage_MultipleDotsInFileName_ShouldUseLastExtension` - Files like test.component.ts
- `testIsCodeFile_ShouldIdentifyCodeFiles` - Parameterized test for code file detection
- `testIsCodeFile_NullFileName_ShouldReturnFalse` - Null input handling for isCodeFile
- `testIsCodeFile_EmptyFileName_ShouldReturnFalse` - Empty string handling for isCodeFile
- `testDetectLanguage_CaseInsensitive` - Verifies extension detection is case-insensitive

**Languages Tested**:
Java, Python, TypeScript, JavaScript, CSS, YAML, Markdown, Go, Rust, Ruby, Kotlin, HTML, Shell, SQL, C++, C, C#, Swift, PHP

### 2. CodeChunkingServiceImplTest
**Location**: `src/test/java/huyphmnat/fdsa/search/CodeChunkingServiceImplTest.java`
**Type**: Unit Test
**Coverage**: `CodeChunkingServiceImpl`

**Test Cases**:
- `testChunkCode_SmallCode_ShouldReturnSingleChunk` - Code smaller than chunk threshold
- `testChunkCode_EmptyCode_ShouldReturnSingleEmptyChunk` - Empty string handling
- `testChunkCode_LargeCode_ShouldSplitIntoMultipleChunks` - Large code (100 classes) splits correctly
- `testChunkCode_SplitsOnLineBreaks` - Verifies chunking respects line boundaries
- `testChunkCode_SingleLongLine_ShouldReturnSingleChunk` - Documents limitation of line-based chunking
- `testChunkCode_MixedLineLengths` - Mixed short and long lines
- `testChunkCode_CodeWithMultipleNewlines` - Preserves consecutive newlines

**Key Insights**:
- Chunk size threshold: 512 tokens * 4 chars = 2048 characters
- Chunking is line-based, so single lines are never split (documented limitation)
- Newlines are preserved in codeChunks

### 3. OpenSearchIndexingServiceTest
**Location**: `src/test/java/huyphmnat/fdsa/search/OpenSearchIndexingServiceTest.java`
**Type**: Integration Test (extends `OpenSearchIntegrationTest`)
**Coverage**: `OpenSearchIndexingService`

**Test Cases**:
- `testIndexCodeFile_ShouldIndexSuccessfully` - Basic single file indexing
- `testIndexCodeFile_WithChunks_ShouldIndexWithChunks` - File with code codeChunks
- `testBulkIndexCodeFiles_ShouldIndexMultipleDocuments` - Bulk indexing (5 files)
- `testBulkIndexCodeFiles_EmptyList_ShouldNotThrowException` - Empty list handling
- `testBulkIndexCodeFiles_LargeBatch_ShouldHandleSuccessfully` - Large batch (150 files)

**Verifies**:
- Document structure in OpenSearch matches expected format
- Chunks are properly indexed with metadata (index, content, start_line, end_line)
- Bulk operations handle large batches
- Search queries can retrieve indexed documents

### 4. RepositoryIndexingServiceImplTest
**Location**: `src/test/java/huyphmnat/fdsa/search/RepositoryIndexingServiceImplTest.java`
**Type**: Integration Test (extends `OpenSearchIntegrationTest`)
**Coverage**: `RepositoryIndexingServiceImpl` (full ingestion pipeline)

**Test Cases**:
- `testIngestRepository_SimpleStructure_ShouldIndexAllFiles` - Flat directory structure
- `testIngestRepository_NestedStructure_ShouldTraverseDirectories` - Nested directories (BFS traversal)
- `testIngestRepository_SkipsNonCodeFiles` - Filters out .png, .pdf files
- `testIngestRepository_SkipsLargeFiles` - Skips files > 10MB
- `testIngestRepository_ChunksLargeFiles` - Large files (>100KB) are chunked
- `testIngestRepository_ContinuesOnFileError` - Error resilience (continues despite failures)
- `testIngestRepository_BulkIndexing` - Batch processing (150 files, batch size 100)

**Mocking Strategy**:
- Uses `@MockBean` for `RepositoryFileService`
- Mocks directory listings and file content
- Tests real OpenSearch integration
- Verifies both service interactions and OpenSearch state

**Key Scenarios Tested**:
- ✅ Simple flat repository structure
- ✅ Nested directory traversal (breadth-first)
- ✅ File filtering (code files only)
- ✅ Size limits (10MB max)
- ✅ Large file chunking (>100KB threshold)
- ✅ Error handling and resilience
- ✅ Bulk indexing with batching

### 5. RepositoryClonedKafkaListenerTest
**Location**: `src/test/java/huyphmnat/fdsa/search/RepositoryClonedKafkaListenerTest.java`
**Type**: Integration Test (extends `BaseIntegrationTest`, uses `@EmbeddedKafka`)
**Coverage**: `RepositoryCloned` Kafka listener

**Test Cases**:
- `testHandleRepositoryCloned_ShouldInvokeIngestionService` - Single event triggers ingestion
- `testHandleRepositoryCloned_MultipleEvents_ShouldProcessAll` - Multiple events processed correctly
- `testHandleRepositoryCloned_IngestionError_ShouldNotThrowException` - Error handling doesn't crash listener

**Kafka Testing**:
- Uses embedded Kafka for real message passing
- Verifies consumer receives and processes events
- Tests asynchronous behavior with appropriate wait times
- Mocks the ingestion service to verify invocations

**Event Flow Tested**:
```
Kafka Producer → RepositoryClonedEvent → Kafka Topic → 
Kafka Consumer (RepositoryCloned) → RepositoryIngestionService
```

## Test Coverage Summary

| Component | Test Type | File Count | Test Methods | Coverage |
|-----------|-----------|------------|--------------|----------|
| LanguageDetectionService | Unit | 1 | 10 | Language detection, code file identification |
| CodeChunkingServiceImpl | Unit | 1 | 7 | Chunking logic, edge cases |
| OpenSearchIndexingService | Integration | 1 | 5 | OpenSearch indexing, bulk operations |
| RepositoryIndexingServiceImpl | Integration | 1 | 7 | Full ingestion pipeline |
| RepositoryCloned (Kafka) | Integration | 1 | 3 | Event handling |
| **Total** | **Mixed** | **5** | **32** | **End-to-end ingestion** |

## Running the Tests

### Run All Ingestion Tests
```bash
./mvnw test -Dtest="huyphmnat.fdsa.search.*Test"
```

### Run Individual Test Classes
```bash
# Unit tests (fast)
./mvnw test -Dtest=LanguageDetectionServiceTest
./mvnw test -Dtest=CodeChunkingServiceImplTest

# Integration tests (slower, require containers)
./mvnw test -Dtest=OpenSearchIndexingServiceTest
./mvnw test -Dtest=RepositoryIndexingServiceImplTest
./mvnw test -Dtest=RepositoryClonedKafkaListenerTest
```

### Run by Test Type
```bash
# Unit tests only
./mvnw test -Dtest="*ServiceTest"

# Integration tests only
./mvnw test -Dtest="*IntegrationTest"
```

## Test Infrastructure

### Base Test Classes Used
- `BaseIntegrationTest` - PostgreSQL + Kafka containers
- `OpenSearchIntegrationTest` - Adds OpenSearch container
- Standard JUnit 5 + Mockito for unit tests

### Testcontainers
- PostgreSQL 18.1
- Kafka (Apache Kafka 4.x)
- OpenSearch 2.11.1

### Key Dependencies
- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers
- Embedded Kafka (Spring Kafka Test)
- OpenSearch Java Client

## Edge Cases Covered

### Input Validation
- ✅ Null inputs
- ✅ Empty strings
- ✅ Files without extensions
- ✅ Case-insensitive handling

### File Processing
- ✅ Code files vs non-code files
- ✅ Small files (< chunk threshold)
- ✅ Large files (> chunk threshold, > max size)
- ✅ Empty files
- ✅ Files with multiple newlines

### Directory Traversal
- ✅ Flat structure
- ✅ Nested structure (multiple levels)
- ✅ Mixed files and directories
- ✅ Empty directories (implicitly tested)

### Error Handling
- ✅ File read errors
- ✅ Directory listing errors
- ✅ Ingestion service errors
- ✅ OpenSearch connection issues (implicitly tested via integration)

### Performance
- ✅ Bulk operations (100 files/batch)
- ✅ Large repositories (150+ files)
- ✅ Large files (chunking)

## Known Limitations (Documented in Tests)

1. **Line-based Chunking**: Single lines longer than chunk size are not split
   - Test: `testChunkCode_SingleLongLine_ShouldReturnSingleChunk`
   - Impact: Very long single lines will create oversized codeChunks
   - Solution: Use semantic chunking in production (as noted in code comments)

2. **Async Wait Times**: Kafka and OpenSearch tests use sleep for synchronization
   - Tests wait 1-5 seconds for async operations
   - Production should use proper synchronization mechanisms

## Future Test Enhancements

1. **Python Workflow Integration**: Test the full Java → Kafka → Python workflow
2. **Search API Tests**: Test querying indexed documents
3. **Performance Benchmarks**: Measure indexing throughput
4. **Stress Tests**: Very large repositories (1000+ files)
5. **Semantic Chunking**: Test AST-based chunking when implemented
6. **Update/Delete Tests**: Test document updates and deletions
7. **Concurrency Tests**: Multiple simultaneous ingestions

## Maintenance Notes

- Tests use mocks for external dependencies where appropriate
- Integration tests are hermetic (no shared state between tests)
- Random UUIDs ensure test isolation
- Cleanup is automatic (Testcontainers handles lifecycle)
- Wait times may need adjustment for slower CI environments

---

**Created**: January 5, 2026
**Test Suite Version**: 1.0
**Framework**: JUnit 5 + Spring Boot Test + Testcontainers

