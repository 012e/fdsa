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
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RepositoryIndexingServiceImpl implements RepositoryIngestionService {

    private final RepositoryFileService repositoryFileService;
    private final OpenSearchIndexingService indexingService;
    private final LanguageDetectionService languageDetectionService;
    private final CodeChunkingService chunkingService;

    private static final int BATCH_SIZE = 100; // Bulk index batch size
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB limit
    private static final int CHUNK_THRESHOLD = 100_000; // Chunk files larger than 100KB

    @Override
    public void ingestRepository(UUID repositoryId, String repositoryIdentifier) {
        log.info("Starting ingestion for repository: {} ({})", repositoryIdentifier, repositoryId);

        try {
            List<CodeFileDocument> documents = new ArrayList<>();
            Queue<String> directoriesToProcess = new LinkedList<>();
            directoriesToProcess.add("/"); // Start from root

            int fileCount = 0;
            int skippedCount = 0;

            while (!directoriesToProcess.isEmpty()) {
                String currentPath = directoriesToProcess.poll();
                log.debug("Processing directory: {}", currentPath);

                try {
                    DirectoryContent content = repositoryFileService.listDirectory(repositoryId, currentPath);

                    for (Entry entry : content.getEntries()) {
                        if (entry instanceof FileEntry fileEntry) {
                            // Process file
                            if (shouldProcessFile(fileEntry)) {
                                CodeFileDocument document = processFile(repositoryId, repositoryIdentifier, fileEntry);
                                if (document != null) {
                                    documents.add(document);
                                    fileCount++;

                                    // Bulk index when batch is full
                                    if (documents.size() >= BATCH_SIZE) {
                                        indexingService.bulkIndexCodeFiles(documents);
                                        documents.clear();
                                    }
                                }
                            } else {
                                skippedCount++;
                            }
                        } else {
                            // Add subdirectory to queue
                            directoriesToProcess.add(entry.getPath());
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to process directory: {}", currentPath, e);
                    // Continue with other directories
                }
            }

            // Index remaining documents
            if (!documents.isEmpty()) {
                indexingService.bulkIndexCodeFiles(documents);
            }

            log.info("Completed ingestion for repository: {}. Indexed: {}, Skipped: {}",
                repositoryIdentifier, fileCount, skippedCount);

        } catch (Exception e) {
            log.error("Failed to ingest repository: {}", repositoryIdentifier, e);
            throw new RuntimeException("Repository ingestion failed", e);
        }
    }

    private boolean shouldProcessFile(FileEntry fileEntry) {
        // Skip very large files
        if (fileEntry.getSize() != null && fileEntry.getSize() > MAX_FILE_SIZE) {
            log.debug("Skipping large file: {} ({})", fileEntry.getName(), fileEntry.getSize());
            return false;
        }

        // Only process code files
        if (!languageDetectionService.isCodeFile(fileEntry.getName())) {
            log.debug("Skipping non-code file: {}", fileEntry.getName());
            return false;
        }

        return true;
    }

    private CodeFileDocument processFile(UUID repositoryId, String repositoryIdentifier, FileEntry fileEntry) {
        try {
            log.debug("Processing file: {}", fileEntry.getPath());

            FileContent fileContent = repositoryFileService.readFile(repositoryId, fileEntry.getPath());
            String content = new String(fileContent.getContent(), StandardCharsets.UTF_8);

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

            // Chunk large files
            if (content.length() > CHUNK_THRESHOLD) {
                List<String> codeChunks = chunkingService.chunkCode(content);
                List<CodeFileDocument.CodeChunk> chunks = new ArrayList<>();

                int currentLine = 1;
                for (int i = 0; i < codeChunks.size(); i++) {
                    String chunkContent = codeChunks.get(i);
                    int linesInChunk = chunkContent.split("\n").length;

                    chunks.add(CodeFileDocument.CodeChunk.builder()
                        .index(i)
                        .content(chunkContent)
                        .startLine(currentLine)
                        .endLine(currentLine + linesInChunk - 1)
                        .build());

                    currentLine += linesInChunk;
                }

                builder.codeChunks(chunks);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Failed to process file: {}", fileEntry.getPath(), e);
            return null; // Skip this file but continue with others
        }
    }

    private String extractFileExtension(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == fileName.length() - 1) {
            return "";
        }

        return fileName.substring(lastDotIndex + 1).toLowerCase();
    }
}
