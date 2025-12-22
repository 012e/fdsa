import { Client } from '@opensearch-project/opensearch';
import { getConfig, type Chunk, type SnippetDocument, type IndexResult } from '@fdsa/shared';

/**
 * Create and return an OpenSearch client
 */
function getOpenSearchClient(): Client {
  const config = getConfig();
  
  return new Client({
    node: `http://${config.opensearch.host}:${config.opensearch.port}`,
    ssl: {
      rejectUnauthorized: false,
    },
  });
}

/**
 * Activity to index the snippet and its chunks into OpenSearch.
 * 
 * @param snippet - A SnippetDocument containing all snippet data
 * @returns Result information including document IDs
 */
export async function indexSnippetToOpenSearch(
  snippet: SnippetDocument
): Promise<IndexResult> {
  const config = getConfig();
  const client = getOpenSearchClient();
  const indexName = config.opensearch.index;

  try {
    // Create index if it doesn't exist
    const indexExists = await client.indices.exists({ index: indexName });
    
    if (!indexExists.body) {
      await client.indices.create({
        index: indexName,
        body: {
          settings: {
            index: {
              number_of_shards: 1,
              number_of_replicas: 0,
              'knn': true,
            },
          },
          mappings: {
            properties: {
              snippet_id: { type: 'keyword' },
              code: { type: 'text' },
              overall_summary: { type: 'text' },
              overall_embedding: {
                type: 'knn_vector',
                dimension: config.openai.embeddingDimensions,
                method: {
                  name: 'hnsw',
                  space_type: 'cosinesimil',
                  engine: 'nmslib',
                },
              },
              chunks: {
                type: 'nested',
                properties: {
                  chunk_index: { type: 'integer' },
                  code: { type: 'text' },
                  summary: { type: 'text' },
                  embedding: {
                    type: 'knn_vector',
                    dimension: config.openai.embeddingDimensions,
                    method: {
                      name: 'hnsw',
                      space_type: 'cosinesimil',
                      engine: 'nmslib',
                    },
                  },
                },
              },
              created_at: { type: 'date' },
              updated_at: { type: 'date' },
            },
          },
        },
      });
    }

    // Prepare document for indexing
    const document = {
      snippet_id: snippet.snippetId,
      code: snippet.code,
      overall_summary: snippet.overallSummary,
      overall_embedding: snippet.overallEmbedding,
      chunks: snippet.chunks.map((chunk) => ({
        chunk_index: chunk.chunkIndex,
        code: chunk.code,
        summary: chunk.summary,
        embedding: chunk.embedding,
      })),
      created_at: snippet.createdAt || new Date(),
      updated_at: snippet.updatedAt || new Date(),
    };

    // Index the document (using snippet_id as the document ID)
    const response = await client.index({
      index: indexName,
      id: snippet.snippetId,
      body: document,
      refresh: 'wait_for',
    });

    return {
      success: true,
      documentId: response.body._id,
    };
  } catch (error) {
    console.error('Error indexing snippet to OpenSearch:', error);
    return {
      success: false,
      error: error instanceof Error ? error.message : String(error),
    };
  }
}
