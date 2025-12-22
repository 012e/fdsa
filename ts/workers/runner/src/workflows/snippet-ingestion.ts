import { proxyActivities, log } from '@temporalio/workflow';
import type * as activities from '../activities/index.js';
import type { SnippetDocument } from '@fdsa/shared';

// Proxy activities with appropriate timeouts and retry policies
const {
  summarizeCode,
  chunkCode,
  summarizeChunks,
  embedText,
  embedTexts,
  indexSnippetToOpenSearch,
} = proxyActivities<typeof activities>({
  startToCloseTimeout: '5 minutes',
  retry: {
    maximumAttempts: 3,
    initialInterval: '1 second',
    maximumInterval: '10 seconds',
  },
});

/**
 * Temporal workflow for ingesting code snippets into the search engine.
 *
 * This workflow:
 * 1. Generates an overall summary of the code
 * 2. Creates an embedding for the overall summary
 * 3. Chunks the code semantically
 * 4. Generates summaries for each chunk
 * 5. Creates embeddings for all chunk summaries
 * 6. Indexes everything into OpenSearch
 */
export async function snippetIngestionWorkflow(
  snippetId: string,
  code: string
): Promise<{ success: boolean; documentId?: string }> {
  log.info('Starting ingestion workflow', { snippetId });

  // Step 1: Generate overall summary
  log.info('Generating overall summary...');
  const overallSummary = await summarizeCode(code);

  // Step 2: Generate embedding for overall summary
  log.info('Generating embedding for overall summary...');
  const overallEmbedding = await embedText(overallSummary);

  // Step 3: Chunk the code
  log.info('Chunking code...');
  const chunks = await chunkCode(code);
  log.info(`Created ${chunks.length} chunks`);

  // Step 4: Generate summaries for each chunk
  log.info('Generating chunk summaries...');
  const chunkSummaries = await summarizeChunks(chunks);

  // Step 5: Generate embeddings for all chunk summaries
  log.info('Generating embeddings for chunk summaries...');
  const chunkEmbeddings = await embedTexts(chunkSummaries);

  // Step 6: Prepare the document
  const snippetDocument: SnippetDocument = {
    snippetId,
    code,
    overallSummary,
    overallEmbedding,
    chunks: chunks.map((chunkCode, index) => ({
      chunkIndex: index,
      code: chunkCode,
      summary: chunkSummaries[index],
      embedding: chunkEmbeddings[index],
    })),
    createdAt: new Date(),
    updatedAt: new Date(),
  };

  // Step 7: Index to OpenSearch
  log.info('Indexing to OpenSearch...');
  const result = await indexSnippetToOpenSearch(snippetDocument);

  if (result.success) {
    log.info('Workflow completed successfully', {
      snippetId,
      documentId: result.documentId,
    });
  } else {
    log.error('Failed to index snippet', { snippetId, error: result.error });
  }

  return result;
}
