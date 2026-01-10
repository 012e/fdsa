# Repository Ingestion Pipeline

## Overview

This module provides a full-text search ingestion pipeline for repository code files using OpenSearch. When a repository is cloned, the system automatically indexes all code files to enable fast full-text search across the repository's content.

## Architecture

The ingestion pipeline follows the modular monolithic architecture pattern with clear separation between public interfaces and internal implementation:

```
search/
├── interfaces/               # Public API
│   └── RepositoryIngestionService.java
└── internal/                 # Internal implementation
    ├── ingestion/
    │   └── RepositoryCloned.java (Kafka listener)
    ├── models/
    │   └── CodeFileDocument.java
    └── services/
        ├── OpenSearchIndexingService.java
        ├── RepositoryIndexingServiceImpl.java
        ├── LanguageDetectionService.java
        ├── CodeChunkingService.java
        └── CodeChunkingServiceImpl.java
```

## Flow

1. **Event Trigger**: Repository cloned → `RepositoryClonedEvent` published to Kafka
2. **Event Handler**: `RepositoryCloned` listener receives event
3. **Ingestion**: `RepositoryIngestionService.ingestRepository()` is called
4. **Repository Traversal**: 
   - Breadth-first traversal of repository directory structure
   - Filters for code files based on extension
   - Skips large files (>10MB)
5. **File Processing**:
   - Read file content
   - Detect programming language from extension
   - Chunk large files (>100KB) for better search granularity
   - Create `CodeFileDocument` with metadata
6. **Indexing**: 
   - Batch index documents (100 per batch) to OpenSearch
   - Uses `code_files` index

## OpenSearch Document Structure

```json
{
  "id": "uuid",
  "repository_id": "uuid",
  "repository_identifier": "owner/repo",
  "file_path": "src/main/java/Main.java",
  "file_name": "Main.java",
  "file_extension": "java",
  "language": "Java",
  "content": "public class Main {...}",
  "size": 1024,
  "codeChunks": [
    {
      "index": 0,
      "content": "chunk content",
      "start_line": 1,
      "end_line": 50
    }
  ],
  "created_at": "2026-01-05T10:00:00Z",
  "updated_at": "2026-01-05T10:00:00Z"
}
```

## Components

### RepositoryIngestionService (Interface)

**Location**: `search.interfaces`

Public interface for ingesting repositories into the search index.

**Methods**:
- `ingestRepository(UUID repositoryId, String repositoryIdentifier)`: Ingest a full repository

### RepositoryIndexingServiceImpl

**Location**: `search.internal.services`

Core implementation of the ingestion pipeline.

**Key Features**:
- Breadth-first directory traversal
- File filtering (code files only, size limits)
- Bulk indexing (100 documents per batch)
- Error resilience (continues on individual file failures)
- Large file chunking

**Configuration**:
- `BATCH_SIZE = 100`: Bulk index batch size
- `MAX_FILE_SIZE = 10MB`: Maximum file size to process
- `CHUNK_THRESHOLD = 100KB`: Chunk files larger than this

### OpenSearchIndexingService

**Location**: `search.internal.services`

Handles low-level OpenSearch operations.

**Methods**:
- `indexCodeFile(CodeFileDocument)`: Index a single file
- `bulkIndexCodeFiles(List<CodeFileDocument>)`: Bulk index multiple files
- `indexSnippet(...)`: Index code snippets (existing functionality)

**Indices**:
- `code_snippets`: For individual code snippets (existing)
- `code_files`: For repository files (new)

### LanguageDetectionService

**Location**: `search.internal.services`

Detects programming language from file extensions.

**Supported Languages**:
- Java ecosystem (Java, Kotlin, Scala, Groovy)
- JavaScript/TypeScript ecosystem
- Python, C/C++, C#, Go, Rust, Ruby, PHP, Swift
- Web (HTML, CSS, SCSS, etc.)
- Shell scripts
- Configuration files (YAML, JSON, XML)
- Markdown, SQL, and more

**Methods**:
- `detectLanguage(String fileName)`: Returns language name or "Unknown"
- `isCodeFile(String fileName)`: Check if file is a code file

### CodeChunkingService

**Location**: `search.internal.services`

Splits large code files into searchable codeChunks.

**Configuration**:
- `CHUNK_SIZE = 512`: Tokens per chunk (approximate)
- `CHARS_PER_TOKEN = 4`: Character-to-token ratio

### CodeFileDocument

**Location**: `search.internal.models`

Document model for code files.

**Fields**:
- Document metadata (id, repository info, file info)
- Content (full file content)
- Language detection results
- Chunks for large files (with line ranges)
- Timestamps

## Usage

### Automatic Ingestion

The pipeline automatically triggers when a repository is cloned:

```java
// Repository cloned → Event published
eventService.publish("repository.cloned", RepositoryClonedEvent.builder()
    .id(repositoryId)
    .identifier("owner/repo")
    .sourceUrl("https://...")
    .filesystemPath("/tmp/repos/...")
    .build());

// Event handler automatically ingests
// No manual intervention required
```

### Manual Ingestion

To manually trigger ingestion (e.g., for re-indexing):

```java
@Autowired
private RepositoryIngestionService ingestionService;

// Ingest a repository
ingestionService.ingestRepository(repositoryId, "owner/repo");
```

## Performance Considerations

### Batch Processing
- Files are indexed in batches of 100 to optimize network calls
- Reduces overhead compared to individual indexing

### File Filtering
- Skips non-code files (binary files, images, etc.)
- Skips very large files (>10MB) to prevent memory issues
- Only processes files with recognized code extensions

### Error Resilience
- Individual file processing errors don't stop the entire ingestion
- Failed files are logged but ingestion continues
- Bulk indexing errors are logged with failure details

### Memory Management
- Files are processed one at a time (not all loaded into memory)
- Large files are chunked to reduce OpenSearch document size
- Batch size limits prevent excessive memory usage

## Monitoring

### Logging

The pipeline provides detailed logging at multiple levels:

```
INFO  - Started ingestion for repository: owner/repo (uuid)
DEBUG - Processing directory: /src/main/java
DEBUG - Processing file: /src/main/java/Main.java
INFO  - Indexed 100 files (batch)
INFO  - Completed ingestion. Indexed: 523, Skipped: 42
```

### Metrics to Monitor

1. **Ingestion Time**: Time to fully index a repository
2. **File Count**: Number of files indexed vs skipped
3. **Error Rate**: Number of files that failed to index
4. **Batch Performance**: Time per bulk index operation

## OpenSearch Index Settings

To create the `code_files` index with optimal settings:

```json
PUT /code_files
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 0,
    "analysis": {
      "analyzer": {
        "code_analyzer": {
          "type": "custom",
          "tokenizer": "standard",
          "filter": ["lowercase", "stop"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": { "type": "keyword" },
      "repository_id": { "type": "keyword" },
      "repository_identifier": { "type": "keyword" },
      "file_path": { 
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "file_name": { 
        "type": "text",
        "fields": {
          "keyword": { "type": "keyword" }
        }
      },
      "file_extension": { "type": "keyword" },
      "language": { "type": "keyword" },
      "content": { 
        "type": "text",
        "analyzer": "code_analyzer"
      },
      "size": { "type": "long" },
      "codeChunks": {
        "type": "nested",
        "properties": {
          "index": { "type": "integer" },
          "content": { 
            "type": "text",
            "analyzer": "code_analyzer"
          },
          "start_line": { "type": "integer" },
          "end_line": { "type": "integer" }
        }
      },
      "created_at": { "type": "date" },
      "updated_at": { "type": "date" }
    }
  }
}
```

## Future Enhancements

1. **Incremental Updates**: Index only changed files on repository updates
2. **Delete Handling**: Remove indexed files when repository is deleted
3. **AST-based Chunking**: Use Abstract Syntax Trees for better code understanding
4. **Semantic Embeddings**: Add vector embeddings for semantic code search
5. **Custom Language Support**: Better detection for domain-specific languages
6. **Index Optimization**: Periodic index optimization and cleanup
7. **Search API**: GraphQL/REST endpoints for searching indexed code
8. **Filtering**: Search by language, file type, repository, etc.
9. **Highlighting**: Return matched code snippets with highlighting
10. **Ranking**: Better relevance scoring for search results

## Troubleshooting

### Repository not being indexed

1. Check Kafka is running and accessible
2. Verify `RepositoryClonedEvent` is being published
3. Check logs for `RepositoryCloned` listener
4. Ensure OpenSearch is running and accessible

### Files being skipped

1. Check file extension is in `LanguageDetectionService`
2. Verify file size is under 10MB limit
3. Look for "Skipping" log messages with reasons

### OpenSearch errors

1. Verify OpenSearch is running: `curl http://localhost:9200`
2. Check index exists: `curl http://localhost:9200/code_files`
3. Review bulk indexing errors in logs
4. Check disk space on OpenSearch node

### Memory issues

1. Reduce `BATCH_SIZE` for lower memory usage
2. Reduce `MAX_FILE_SIZE` to skip larger files
3. Increase JVM heap size if needed

## Testing

To test the ingestion pipeline:

```java
@SpringBootTest
class RepositoryIngestionServiceTest {
    
    @Autowired
    private RepositoryIngestionService ingestionService;
    
    @Test
    void testIngestRepository() {
        // Clone a test repository first
        UUID repoId = cloneTestRepository();
        
        // Ingest it
        ingestionService.ingestRepository(repoId, "test/repo");
        
        // Verify in OpenSearch
        // ... search queries ...
    }
}
```

## Configuration

All configuration is centralized in `application.yaml`:

```yaml
spring:
  opensearch:
    host: ${OPENSEARCH_HOST:localhost}
    port: ${OPENSEARCH_PORT:9200}
    scheme: ${OPENSEARCH_SCHEME:http}
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

## Dependencies

- Spring Boot 3.x
- OpenSearch Java Client
- Spring Kafka
- Lombok (for boilerplate reduction)

