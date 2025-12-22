/**
 * Shared types for the snippet ingestion pipeline
 */

/**
 * Represents a chunk of code with its metadata
 */
export interface Chunk {
  chunkIndex: number;
  code: string;
  summary: string;
  embedding: number[];
}

/**
 * Represents a complete snippet document for OpenSearch
 */
export interface SnippetDocument {
  snippetId: string;
  code: string;
  overallSummary: string;
  overallEmbedding: number[];
  chunks: Chunk[];
  createdAt?: Date;
  updatedAt?: Date;
}

/**
 * Result of indexing operation
 */
export interface IndexResult {
  success: boolean;
  documentId?: string;
  error?: string;
}
