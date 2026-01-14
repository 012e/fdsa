package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.repository.dtos.DirectoryContent;
import huyphmnat.fdsa.repository.dtos.Entry;
import huyphmnat.fdsa.repository.dtos.FileContent;
import huyphmnat.fdsa.repository.dtos.FileEntry;
import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import huyphmnat.fdsa.search.interfaces.RepositoryIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class RepositoryIndexingServiceImpl implements RepositoryIngestionService {

    private final RepositoryFileService repositoryFileService;
    private final OpenSearchIndexingService indexingService;
    private final LanguageDetectionService languageDetectionService;
    private final CodeChunkingService chunkingService;
    private final EmbeddingModel embeddingModel;
    private final CodeSummarizationService summarizationService;

    // Concurrency controls
    // Use a FixedThreadPool to control rate limits (e.g., 10-20 concurrent LLM calls)
    // If using Java 21+ and no rate limits, use Executors.newVirtualThreadPerTaskExecutor()
    private final ExecutorService executorService = Executors.newFixedThreadPool(15); 
    
    private static final int BATCH_SIZE = 100;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    private static final int MAX_EMBEDDING_CHARS = 8000;

    @Override
    public void ingestRepository(UUID repositoryId, String repositoryIdentifier) {
        log.info("Starting parallel ingestion for repository: {} ({})", repositoryIdentifier, repositoryId);
        long startTime = System.currentTimeMillis();

        // Thread-safe collection for the current batch
        List<CodeFileDocument> currentBatch = Collections.synchronizedList(new ArrayList<>());
        // Track futures to wait for completion
        List<CompletableFuture<Void>> futures = new ArrayList<>();
        
        // Counters for logging
        AtomicInteger fileCount = new AtomicInteger(0);
        AtomicInteger skippedCount = new AtomicInteger(0);

        try {
            Queue<String> directoriesToProcess = new LinkedList<>();
            directoriesToProcess.add("/");

            while (!directoriesToProcess.isEmpty()) {
                String currentPath = directoriesToProcess.poll();

                try {
                    DirectoryContent content = repositoryFileService.listDirectory(repositoryId, currentPath);

                    for (Entry entry : content.getEntries()) {
                        if (entry instanceof FileEntry fileEntry) {
                            if (shouldProcessFile(fileEntry)) {
                                // Submit file processing to the thread pool
                                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                                    try {
                                        CodeFileDocument doc = processFile(repositoryId, repositoryIdentifier, fileEntry);
                                        if (doc != null) {
                                            fileCount.incrementAndGet();
                                            addDocumentToBatch(doc, currentBatch);
                                        }
                                    } catch (Exception e) {
                                        log.error("Error processing file asynchronously: {}", fileEntry.getPath(), e);
                                    }
                                }, executorService);
                                
                                futures.add(future);
                            } else {
                                skippedCount.incrementAndGet();
                            }
                        } else {
                            directoriesToProcess.add(entry.getPath());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process directory: {}", currentPath, e);
                }
            }

            // Wait for all processing tasks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            // Index any remaining documents in the batch
            flushRemainingBatch(currentBatch);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Completed ingestion for {}. Indexed: {}, Skipped: {}, Time: {}ms",
                    repositoryIdentifier, fileCount.get(), skippedCount.get(), duration);

        } catch (Exception e) {
            log.error("Failed to ingest repository: {}", repositoryIdentifier, e);
            throw new RuntimeException("Repository ingestion failed", e);
        }
    }

    /**
     * Thread-safe method to add documents to batch and flush if size limit reached.
     */
    private synchronized void addDocumentToBatch(CodeFileDocument doc, List<CodeFileDocument> batch) {
        batch.add(doc);
        if (batch.size() >= BATCH_SIZE) {
            log.debug("Batch size reached ({}), flushing to index...", batch.size());
            // Create a copy to index so we can clear the main list immediately
            List<CodeFileDocument> batchToIndex = new ArrayList<>(batch);
            batch.clear();
            
            // Indexing can happen on the current thread (the worker thread)
            // or be offloaded. Here we do it on the worker thread.
            try {
                indexingService.bulkIndexCodeFiles(batchToIndex);
            } catch (Exception e) {
                log.error("Failed to bulk index batch", e);
                // In production, consider a retry mechanism or Dead Letter Queue here
            }
        }
    }

    private synchronized void flushRemainingBatch(List<CodeFileDocument> batch) {
        if (!batch.isEmpty()) {
            log.debug("Flushing remaining {} documents...", batch.size());
            try {
                indexingService.bulkIndexCodeFiles(new ArrayList<>(batch));
                batch.clear();
            } catch (Exception e) {
                log.error("Failed to flush remaining documents", e);
            }
        }
    }

    private boolean shouldProcessFile(FileEntry fileEntry) {
        if (fileEntry.getSize() != null && fileEntry.getSize() > MAX_FILE_SIZE) return false;
        return languageDetectionService.isCodeFile(fileEntry.getName());
    }

    private CodeFileDocument processFile(UUID repositoryId, String repositoryIdentifier, FileEntry fileEntry) {
        FileContent fileContent = repositoryFileService.readFile(repositoryId, fileEntry.getPath());
        String content = fileContent.getContent();

        String fileExtension = extractFileExtension(fileEntry.getName());
        String language = languageDetectionService.detectLanguage(fileEntry.getName());

        CodeFileDocument.CodeFileDocumentBuilder builder = CodeFileDocument.builder()
                .id(UUID.randomUUID())
                .repositoryId(repositoryId)
                .repositoryIdentifier(repositoryIdentifier)
                .filePath(fileEntry.getPath())
                .fileName(fileEntry.getName())
                .fileExtension(fileExtension)
                .language(language)
                .content(content)
                .size(fileEntry.getSize())
                .createdAt(Instant.now())
                .updatedAt(Instant.now());

        List<CodeFileDocument.CodeChunk> chunks = chunkingService.chunkCodeWithMetadata(content);
        builder.codeChunks(chunks);

        String summary = summarizationService.summarizeCode(content, language, fileEntry.getPath());
        builder.contentSummary(summary);

        List<Float> contentEmbedding = generateEmbedding(summary);
        builder.contentEmbedding(contentEmbedding);
        
        return builder.build();
    }

    private List<Float> generateEmbedding(String text) {
        try {
            String truncatedText = text.length() > MAX_EMBEDDING_CHARS 
                ? text.substring(0, MAX_EMBEDDING_CHARS) 
                : text;
            
            EmbeddingRequest request = new EmbeddingRequest(List.of(truncatedText), null);
            EmbeddingResponse response = embeddingModel.call(request);

            if (response.getResults().isEmpty()) return new ArrayList<>();

            float[] embedding = response.getResult().getOutput();
            List<Float> floatEmbedding = new ArrayList<>(embedding.length);
            for (float value : embedding) floatEmbedding.add(value);

            return floatEmbedding;
        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            // Return empty or throw depending on if you want to fail the whole file
            return new ArrayList<>(); 
        }
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) return "";
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) return "";
        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}