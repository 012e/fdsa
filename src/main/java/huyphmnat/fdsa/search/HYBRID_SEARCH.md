# Hybrid Search with RRF (Reciprocal Rank Fusion)

## Overview

The code search engine now supports **hybrid search**, which combines traditional full-text keyword search with semantic vector search using OpenSearch's RRF (Reciprocal Rank Fusion) feature. This provides more relevant results by leveraging both exact keyword matching and semantic understanding.

## What is Hybrid Search?

Hybrid search combines two search strategies:

1. **Keyword Search (BM25)**: Traditional full-text search that matches exact terms and phrases
2. **Vector Search (kNN)**: Semantic search using embeddings that understands meaning and context

The results from both searches are merged using **RRF (Reciprocal Rank Fusion)**, which intelligently combines rankings from multiple sources to produce a unified result set.

## Architecture

```
User Query
    ↓
    ├─→ Keyword Search (BM25) ──→ Rankings 1
    │   (multi_match on content, file_name, file_path)
    │
    └─→ Vector Search (kNN) ────→ Rankings 2
        (cosine similarity on embeddings)
        
        Both Rankings ──→ RRF Merge ──→ Final Results
```

## Features

### 1. Full-Text Search (Default)
- Fast BM25-based keyword matching
- Field boosting (content^3, file_name^2, file_path)
- Highlighting support
- Filters: repository, language, file path patterns

### 2. Hybrid Search (Optional)
- Combines keyword and vector search
- Semantic understanding of queries
- Better handling of synonyms and related concepts
- RRF-based result fusion

### 3. Flexible Embedding Options
- Auto-generate embeddings from query text
- Or provide pre-computed query embeddings
- Falls back to keyword search if embeddings unavailable

## Configuration

### Environment Variables

```bash
# Enable hybrid search
SEARCH_EMBEDDINGS_ENABLED=true

# OpenAI API Key for generating embeddings
OPENAI_API_KEY=sk-...

# Embedding model (default: text-embedding-3-small)
SEARCH_EMBEDDINGS_MODEL=text-embedding-3-small

# Embedding dimension (default: 1536)
SEARCH_EMBEDDINGS_DIMENSION=1536
```

### application.yaml

```yaml
search:
  embeddings:
    enabled: ${SEARCH_EMBEDDINGS_ENABLED:false}
    api-key: ${OPENAI_API_KEY:}
    model: ${SEARCH_EMBEDDINGS_MODEL:text-embedding-3-small}
    dimension: ${SEARCH_EMBEDDINGS_DIMENSION:1536}
```

## OpenSearch Index Configuration

The index schema includes kNN vector fields:

```json
{
  "content_embedding": {
    "type": "knn_vector",
    "dimension": 1536,
    "method": {
      "name": "hnsw",
      "space_type": "cosinesimil",
      "engine": "nmslib",
      "parameters": {
        "ef_construction": 128,
        "m": 16
      }
    }
  },
  "chunks": {
    "type": "nested",
    "properties": {
      "embedding": {
        "type": "knn_vector",
        "dimension": 1536,
        "method": { ... }
      }
    }
  }
}
```

### Index Settings

```json
{
  "settings": {
    "index.knn": true
  }
}
```

## API Usage

### GraphQL Query

```graphql
query SearchCode {
  searchCode(
    query: "Spring Boot application configuration"
    hybridSearch: true
    repositoryIdentifier: "myorg/myrepo"
    page: 0
    size: 10
  ) {
    results {
      id
      fileName
      filePath
      content
      score
      highlights {
        content
      }
    }
    totalHits
    page
    totalPages
  }
}
```

### Java API

```java
CodeSearchRequest request = CodeSearchRequest.builder()
    .query("authentication middleware")
    .hybridSearch(true)  // Enable hybrid search
    .repositoryIdentifier("myorg/myrepo")
    .language("java")
    .page(0)
    .size(10)
    .highlightFields(List.of("content", "file_name"))
    .build();

CodeSearchResponse response = codeSearchService.searchCode(request);
```

### With Pre-computed Embedding

```java
List<Float> queryEmbedding = embeddingService.generateEmbedding("authentication middleware");

CodeSearchRequest request = CodeSearchRequest.builder()
    .query("authentication middleware")
    .hybridSearch(true)
    .queryEmbedding(queryEmbedding)  // Provide pre-computed embedding
    .build();
```

## How RRF Works

RRF (Reciprocal Rank Fusion) merges rankings using the formula:

```
RRF_score(doc) = Σ [ 1 / (k + rank_i(doc)) ]
```

Where:
- `k` is a constant (typically 60)
- `rank_i(doc)` is the rank of the document in the i-th result set

This gives higher scores to documents that appear highly ranked in multiple result sets.

## Embedding Service Implementation

The current implementation provides a stub `EmbeddingService` that needs to be connected to an actual embedding provider:

### Option 1: OpenAI API (Recommended)

```java
@Service
public class OpenAIEmbeddingService implements EmbeddingService {
    private final RestClient openAIClient;
    
    @Override
    public List<Float> generateEmbedding(String text) {
        // Call OpenAI Embeddings API
        // https://api.openai.com/v1/embeddings
    }
}
```

### Option 2: Local Model (Sentence Transformers)

```java
@Service
public class LocalEmbeddingService implements EmbeddingService {
    private final SentenceTransformerModel model;
    
    @Override
    public List<Float> generateEmbedding(String text) {
        // Use local transformer model
    }
}
```

### Option 3: Dedicated Service

```java
@Service
public class RemoteEmbeddingService implements EmbeddingService {
    private final RestClient embeddingServiceClient;
    
    @Override
    public List<Float> generateEmbedding(String text) {
        // Call dedicated embedding service
    }
}
```

## Indexing Documents with Embeddings

To use hybrid search, documents must be indexed with embeddings:

```java
// Generate embedding for content
List<Float> contentEmbedding = embeddingService.generateEmbedding(fileContent);

// Generate embeddings for chunks
List<CodeFileDocument.CodeChunk> chunks = chunkingService.chunkCode(fileContent);
for (CodeFileDocument.CodeChunk chunk : chunks) {
    List<Float> chunkEmbedding = embeddingService.generateEmbedding(chunk.getContent());
    chunk.setEmbedding(chunkEmbedding);
}

// Create document
CodeFileDocument doc = CodeFileDocument.builder()
    .content(fileContent)
    .contentEmbedding(contentEmbedding)
    .codeChunks(chunks)
    .build();

// Index document
indexingService.indexDocument(doc);
```

## Performance Considerations

### 1. Embedding Generation
- Embeddings are generated on-demand for queries
- Consider caching frequently used query embeddings
- Batch embedding generation for indexing

### 2. Vector Search Performance
- kNN search is approximate (HNSW algorithm)
- Tune HNSW parameters: `ef_construction`, `m`
- Higher values = better accuracy, slower indexing

### 3. Index Size
- Each embedding adds ~6KB per document (1536 floats)
- Plan storage accordingly for large codebases

### 4. Query Latency
- Keyword search: ~10-50ms
- Hybrid search: ~50-200ms (includes embedding generation)
- Consider timeout configurations

## Fallback Behavior

The system gracefully degrades when embeddings are unavailable:

1. If `hybridSearch=true` but embeddings disabled → Falls back to keyword search
2. If embedding generation fails → Falls back to keyword search
3. If OpenSearch hybrid query fails → Falls back to keyword search

## Example Queries

### Keyword Search (Default)
```
Query: "SpringBootApplication annotation"
Matches: Exact term matches, ignores semantic meaning
```

### Hybrid Search
```
Query: "application startup configuration"
Matches: 
- Keyword: "application", "startup", "configuration"
- Semantic: @SpringBootApplication, main methods, config files
```

### Semantic Search Benefits
```
Query: "error handling"
Matches:
- try/catch blocks
- exception handlers
- @ExceptionHandler annotations
- error response classes
```

## Testing

Run the hybrid search tests:

```bash
./mvnw test -Dtest=CodeSearchServiceHybridSearchTest
```

## Monitoring

Key metrics to monitor:
- Embedding generation latency
- Hybrid query latency
- Fallback rate (hybrid → keyword)
- Search relevance (manual evaluation)

## Limitations

1. **OpenSearch Version**: Requires OpenSearch 2.9+ for native hybrid search
2. **Java Client Support**: Limited support for hybrid queries in Java client (uses JSON)
3. **Embedding Model**: Must match dimension (1536 for text-embedding-3-small)
4. **Cost**: API calls for embedding generation (if using external service)

## Future Enhancements

1. **Query Caching**: Cache embeddings for popular queries
2. **Chunk-Level Search**: Search within code chunks with hybrid approach
3. **Custom RRF Parameters**: Tune RRF constant `k` per use case
4. **Multi-Vector Search**: Separate embeddings for code vs comments
5. **Re-ranking**: Post-process results with a re-ranking model
6. **Analytics**: Track which search type produces better results

## References

- [OpenSearch kNN Plugin](https://opensearch.org/docs/latest/search-plugins/knn/index/)
- [OpenSearch Hybrid Search](https://opensearch.org/docs/latest/search-plugins/hybrid-search/)
- [RRF Algorithm](https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf)
- [OpenAI Embeddings](https://platform.openai.com/docs/guides/embeddings)

## Support

For issues or questions:
1. Check if `index.knn: true` is set in index settings
2. Verify OpenSearch version supports hybrid search
3. Check embedding service configuration
4. Review logs for fallback warnings

