package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeChunkingServiceImpl implements CodeChunkingService {

    private final EmbeddingModel embeddingModel;

    @Value("${search.embeddings.enabled:false}")
    private boolean embeddingsEnabled;

    private static final int CHUNK_SIZE = 512; // tokens
    private static final int CHARS_PER_TOKEN = 4; // rough estimate

    @Override
    public List<String> chunkCode(String code) {
        log.debug("Chunking code...");

        // Simple chunking by approximate token count
        // For production, consider using a proper tokenizer
        int chunkSizeInChars = CHUNK_SIZE * CHARS_PER_TOKEN;
        List<String> chunks = new ArrayList<>();

        if (code.length() <= chunkSizeInChars) {
            chunks.add(code);
            return chunks;
        }

        // Split by lines to avoid breaking in the middle of lines
        String[] lines = code.split("\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String line : lines) {
            if (currentChunk.length() + line.length() + 1 > chunkSizeInChars && !currentChunk.isEmpty()) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(line).append("\n");
        }

        if (!currentChunk.isEmpty()) {
            chunks.add(currentChunk.toString());
        }

        log.debug("Created {} chunks", chunks.size());
        return chunks;
    }

    @Override
    public List<CodeFileDocument.CodeChunk> chunkCodeWithMetadata(String code) {
        log.info("Chunking code with metadata and embeddings...");

        List<String> chunkStrings = chunkCode(code);
        List<CodeFileDocument.CodeChunk> chunks = new ArrayList<>();

        // Generate embeddings for all chunks in batch if enabled
        List<List<Float>> embeddings = new ArrayList<>();
        if (embeddingsEnabled && !chunkStrings.isEmpty()) {
            try {
                embeddings = generateEmbeddingsBatch(chunkStrings);
            } catch (Exception e) {
                log.error("Failed to generate embeddings for chunks, proceeding without embeddings", e);
                // Fill with empty embeddings
                for (int i = 0; i < chunkStrings.size(); i++) {
                    embeddings.add(new ArrayList<>());
                }
            }
        } else {
            // Fill with empty embeddings if disabled
            for (int i = 0; i < chunkStrings.size(); i++) {
                embeddings.add(new ArrayList<>());
            }
        }

        // Track line numbers
        int currentLine = 1;
        for (int i = 0; i < chunkStrings.size(); i++) {
            String chunkContent = chunkStrings.get(i);
            int linesInChunk = chunkContent.split("\n").length;

            chunks.add(CodeFileDocument.CodeChunk.builder()
                .index(i)
                .content(chunkContent)
                .startLine(currentLine)
                .endLine(currentLine + linesInChunk - 1)
                .embedding(embeddings.get(i))
                .build());

            currentLine += linesInChunk;
        }

        log.info("Created {} chunks with metadata", chunks.size());
        return chunks;
    }

    private List<List<Float>> generateEmbeddingsBatch(List<String> texts) {
        log.debug("Generating embeddings for {} texts", texts.size());

        try {
            // Create embedding request for batch
            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = embeddingModel.call(request);

            if (response.getResults().isEmpty()) {
                log.warn("No embedding results returned for batch");
                List<List<Float>> emptyEmbeddings = new ArrayList<>();
                for (int i = 0; i < texts.size(); i++) {
                    emptyEmbeddings.add(new ArrayList<>());
                }
                return emptyEmbeddings;
            }

            // Convert all embeddings to List<Float>
            List<List<Float>> embeddings = new ArrayList<>();
            for (var result : response.getResults()) {
                float[] embedding = result.getOutput();
                List<Float> floatEmbedding = new ArrayList<>(embedding.length);
                for (float value : embedding) {
                    floatEmbedding.add(value);
                }
                embeddings.add(floatEmbedding);
            }

            log.debug("Successfully generated {} embeddings", embeddings.size());
            return embeddings;

        } catch (Exception e) {
            log.error("Failed to generate embeddings for batch", e);
            throw new RuntimeException("Failed to generate embeddings", e);
        }
    }
}

