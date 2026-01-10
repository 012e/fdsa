# Hybrid Search Implementation - Summary

## What Was Added

This implementation adds **hybrid search with RRF (Reciprocal Rank Fusion)** to the code search engine, combining traditional full-text keyword search with semantic vector search using OpenSearch.

## Key Changes

### 1. Data Model Updates

**FieldNames.java**
- Added `CONTENT_EMBEDDING` and `CHUNK_EMBEDDING` field constants

**CodeFileDocument.java**
- Added `contentEmbedding` field (List<Float>) for document-level embeddings
- Added `embedding` field to `CodeChunk` for chunk-level embeddings

**CodeSearchRequest.java**
- Added `hybridSearch` boolean flag (default: false)
- Added `queryEmbedding` field for pre-computed embeddings

### 2. OpenSearch Index Configuration

**OpenSearchIndexInitializer.java**
- Updated index mapping with kNN vector fields:
  - `content_embedding`: knn_vector field (dimension: 1536)
  - `chunks.embedding`: knn_vector field (dimension: 1536)
- Configured HNSW algorithm with cosine similarity
- Enabled `index.knn: true` setting

### 3. Embedding Service

**EmbeddingService.java** (Interface)
- `generateEmbedding(String text)`: Generate embedding for text
- `isAvailable()`: Check if service is configured

**EmbeddingServiceImpl.java** (Implementation)
- Stub implementation that needs to be connected to an actual embedding provider
- Configuration via `search.embeddings.*` properties
- Currently returns null (placeholder for actual implementation)

### 4. Hybrid Search Implementation

**CodeSearchServiceImpl.java**
- `buildHybridSearchRequest()`: Creates hybrid search using RRF
- `buildHybridQuery()`: Combines keyword and vector queries using native `HybridQuery` API
- Graceful fallback to keyword search if embeddings unavailable
- Uses OpenSearch Java client's native APIs:
  ```java
  Query hybridQuery = HybridQuery.of(h -> h
      .queries(queries)
  ).toQuery();
  ```

### 5. REST API Updates

**CodeSearchController.java**
- Added `hybridSearch` query parameter to GET `/api/search/code`
- POST endpoint already supports full `CodeSearchRequest` with all fields

### 6. Configuration

**application.yaml**
```yaml
search:
  embeddings:
    enabled: ${SEARCH_EMBEDDINGS_ENABLED:false}
    api-key: ${OPENAI_API_KEY:}
    model: text-embedding-3-small
    dimension: 1536
```

### 7. Documentation

**HYBRID_SEARCH.md**
- Complete guide on hybrid search functionality
- Architecture overview
- API usage examples
- Configuration instructions
- Performance considerations

## How It Works

1. **User submits search query** with `hybridSearch=true`
2. **Query embedding is generated** (if not provided)
3. **Two parallel searches are executed**:
   - **Keyword search**: BM25 on content, file_name, file_path
   - **Vector search**: kNN on content_embedding
4. **Results are merged using RRF** (Reciprocal Rank Fusion)
5. **Filters are applied** (repository, language, file path)
6. **Results returned** with relevance scores

## Query Examples

### Keyword Search (Default)
```bash
GET /api/search/code?q=SpringBootApplication&page=0&size=10
```

### Hybrid Search
```bash
GET /api/search/code?q=authentication%20middleware&hybridSearch=true&page=0&size=10
```

### With Filters
```bash
GET /api/search/code?q=error%20handling&hybridSearch=true&language=java&repositoryIdentifier=myorg/myrepo
```

## OpenSearch Native API Usage

The implementation uses OpenSearch Java client's native APIs:

```java
// Build keyword query
Query keywordQuery = MultiMatchQuery.of(m -> m
    .query(request.getQuery())
    .fields("content^3", "file_name^2", "file_path")
).toQuery();

// Build vector query
Query vectorQuery = KnnQuery.of(k -> k
    .field(FieldNames.CONTENT_EMBEDDING)
    .vector(queryEmbedding)
    .k(request.getSize() * 2)
).toQuery();

// Combine with HybridQuery
Query hybridQuery = HybridQuery.of(h -> h
    .queries(List.of(keywordQuery, vectorQuery))
).toQuery();
```

## Next Steps

To make hybrid search fully functional:

### 1. Implement Embedding Service

Choose one of:
- **OpenAI API**: Use OpenAI's text-embedding-3-small model
- **Local Model**: Use Sentence Transformers or ONNX Runtime
- **Dedicated Service**: Deploy a separate embedding service

### 2. Generate Embeddings During Indexing

Update the repository ingestion pipeline to:
1. Generate embeddings for file content
2. Generate embeddings for code chunks
3. Store embeddings in OpenSearch documents

### 3. Test with Real Data

1. Index repositories with embeddings
2. Test semantic search queries
3. Compare keyword vs hybrid search results
4. Tune RRF parameters if needed

### 4. Monitor Performance

Track:
- Embedding generation latency
- Search query latency
- Hybrid vs keyword search usage
- Result relevance metrics

## Testing

Run the hybrid search tests:
```bash
./mvnw test -Dtest=CodeSearchServiceHybridSearchTest
```

The tests verify:
- Keyword-only search works
- Hybrid search falls back gracefully when embeddings unavailable
- Pre-computed embeddings are used when provided
- Filters work with hybrid search

## Dependencies

No new dependencies were added. The implementation uses:
- OpenSearch Java Client (existing)
- Spring Boot (existing)
- Lombok (existing)

The embedding service implementation will require additional dependencies based on the chosen provider (e.g., OpenAI SDK, ONNX Runtime, etc.).

## Benefits of This Implementation

1. **Native API Usage**: Uses OpenSearch Java client's native `HybridQuery` API
2. **Graceful Degradation**: Falls back to keyword search if embeddings unavailable
3. **Flexible Configuration**: Enable/disable via configuration
4. **Pre-computed Embeddings**: Support for providing embeddings directly
5. **Filter Support**: All existing filters work with hybrid search
6. **Highlighting**: Full-text highlighting still works
7. **Type-Safe**: Compile-time type checking with Java APIs

## References

- [OpenSearch Hybrid Search](https://opensearch.org/docs/latest/search-plugins/hybrid-search/)
- [OpenSearch kNN Plugin](https://opensearch.org/docs/latest/search-plugins/knn/index/)
- [OpenSearch Java Client](https://opensearch.org/docs/latest/clients/java/)
- [RRF Algorithm](https://plg.uwaterloo.ca/~gvcormac/cormacksigir09-rrf.pdf)

