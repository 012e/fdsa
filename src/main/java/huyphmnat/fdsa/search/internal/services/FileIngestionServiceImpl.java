package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.repository.dtos.FileContent;
import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import huyphmnat.fdsa.search.Indexes;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import huyphmnat.fdsa.search.interfaces.FileIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.DeleteByQueryRequest;
import org.opensearch.client.opensearch.core.DeleteByQueryResponse;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileIngestionServiceImpl implements FileIngestionService {

    private final RepositoryFileService repositoryFileService;
    private final OpenSearchIndexingService indexingService;
    private final OpenSearchClient openSearchClient;
    private final LanguageDetectionService languageDetectionService;
    private final CodeChunkingService chunkingService;
    private final EmbeddingModel embeddingModel;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB limit

    @Override
    public void indexFile(UUID repositoryId, String repositoryIdentifier, String filePath) {
        log.info("Indexing file: {} from repository: {}", filePath, repositoryIdentifier);

        try {
            // Check if it's a code file
            String fileName = extractFileName(filePath);
            if (!languageDetectionService.isCodeFile(fileName)) {
                log.debug("Skipping non-code file: {}", filePath);
                return;
            }

            // Read file content
            FileContent fileContent = repositoryFileService.readFile(repositoryId, filePath);
            
            // Skip large files
            if (fileContent.getSize() != null && fileContent.getSize() > MAX_FILE_SIZE) {
                log.warn("Skipping large file: {} ({})", filePath, fileContent.getSize());
                return;
            }

            String content = fileContent.getContent();
            String fileExtension = extractFileExtension(fileName);
            String language = languageDetectionService.detectLanguage(fileName);

            // Build document
            CodeFileDocument.CodeFileDocumentBuilder builder = CodeFileDocument.builder()
                    .id(UUID.randomUUID())
                    .repositoryId(repositoryId)
                    .repositoryIdentifier(repositoryIdentifier)
                    .filePath(filePath)
                    .fileName(fileName)
                    .fileExtension(fileExtension)
                    .language(language)
                    .content(content)
                    .size(fileContent.getSize())
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now());

            // Chunk the code
            List<CodeFileDocument.CodeChunk> chunks = chunkingService.chunkCodeWithMetadata(content);
            builder.codeChunks(chunks);

            // Generate embedding
            List<Float> contentEmbedding = generateEmbedding(content);
            builder.contentEmbedding(contentEmbedding);

            CodeFileDocument document = builder.build();

            // First, remove any existing document for this file path
            removeFileByPath(repositoryId, filePath);

            // Then index the new document
            indexingService.indexCodeFile(document);

            log.info("Successfully indexed file: {}", filePath);

        } catch (Exception e) {
            log.error("Failed to index file: {}", filePath, e);
            throw new RuntimeException("Failed to index file: " + filePath, e);
        }
    }

    @Override
    public void removeFile(UUID repositoryId, String filePath) {
        log.info("Removing file from index: {} from repository: {}", filePath, repositoryId);

        try {
            removeFileByPath(repositoryId, filePath);
            log.info("Successfully removed file from index: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to remove file from index: {}", filePath, e);
            throw new RuntimeException("Failed to remove file from index: " + filePath, e);
        }
    }

    @Override
    public void removeFolder(UUID repositoryId, String folderPath) {
        log.info("Removing folder from index: {} from repository: {}", folderPath, repositoryId);

        try {
            // Normalize folder path
            String normalizedPath = folderPath.endsWith("/") ? folderPath : folderPath + "/";

            // Delete all documents with filePath starting with the folder path
            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                    .index(Indexes.CODE_FILE_INDEX)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .term(t -> t
                                                    .field("repositoryId")
                                                    .value(FieldValue.of(repositoryId.toString()))
                                            )
                                    )
                                    .must(m -> m
                                            .prefix(p -> p
                                                    .field("filePath.keyword")
                                                    .value(normalizedPath)
                                            )
                                    )
                            )
                    )
            );

            DeleteByQueryResponse response = openSearchClient.deleteByQuery(request);
            log.info("Removed {} files from folder: {}", response.deleted(), folderPath);

        } catch (Exception e) {
            log.error("Failed to remove folder from index: {}", folderPath, e);
            throw new RuntimeException("Failed to remove folder from index: " + folderPath, e);
        }
    }

    private void removeFileByPath(UUID repositoryId, String filePath) {
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(d -> d
                    .index(Indexes.CODE_FILE_INDEX)
                    .query(q -> q
                            .bool(b -> b
                                    .must(m -> m
                                            .term(t -> t
                                                    .field("repositoryId")
                                                    .value(FieldValue.of(repositoryId.toString()))
                                            )
                                    )
                                    .must(m -> m
                                            .term(t -> t
                                                    .field("filePath.keyword")
                                                    .value(FieldValue.of(filePath))
                                            )
                                    )
                            )
                    )
            );

            DeleteByQueryResponse response = openSearchClient.deleteByQuery(request);
            log.debug("Deleted {} documents for file: {}", response.deleted(), filePath);

        } catch (Exception e) {
            log.error("Failed to remove file by path: {}", filePath, e);
            // Don't throw exception here as this is cleanup before indexing
        }
    }

    private List<Float> generateEmbedding(String text) {
        try {
            log.debug("Generating embedding for text of length: {}", text.length());

            EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
            EmbeddingResponse response = embeddingModel.call(request);

            if (response.getResults().isEmpty()) {
                log.warn("No embedding results returned");
                return new ArrayList<>();
            }

            // Convert float[] to List<Float>
            float[] embedding = response.getResult().getOutput();
            List<Float> floatEmbedding = new ArrayList<>(embedding.length);
            for (float value : embedding) {
                floatEmbedding.add(value);
            }

            log.debug("Successfully generated embedding with dimension: {}", floatEmbedding.size());
            return floatEmbedding;

        } catch (Exception e) {
            log.error("Failed to generate embedding", e);
            throw new RuntimeException("Failed to generate embedding", e);
        }
    }

    private String extractFileName(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return "";
        }
        int lastSlashIndex = filePath.lastIndexOf('/');
        return lastSlashIndex == -1 ? filePath : filePath.substring(lastSlashIndex + 1);
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
