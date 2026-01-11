package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.dtos.CodeFileDocument;

import java.util.List;

public interface CodeChunkingService {
    /**
     * Chunk code into smaller pieces (simple string chunks)
     */
    List<String> chunkCode(String code);

    /**
     * Chunk code and create CodeChunk objects with embeddings and line numbers
     */
    List<CodeFileDocument.CodeChunk> chunkCodeWithMetadata(String code);
}
