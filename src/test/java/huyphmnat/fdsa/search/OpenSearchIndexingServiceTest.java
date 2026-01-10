package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import huyphmnat.fdsa.search.internal.services.OpenSearchIndexingService;
import net.datafaker.Faker;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

class OpenSearchIndexingServiceTest extends OpenSearchIntegrationTest {

    @Autowired
    private OpenSearchIndexingService indexingService;

    @Autowired
    private OpenSearchClient openSearchClient;

    private static final String FILES_INDEX_NAME = Indexes.CODE_FILE_INDEX;

    @Test
    void testIndexCodeFile_ShouldIndexSuccessfully() throws Exception {
        // Given
        var repositoryId = UUID.randomUUID();
        var documentId = UUID.randomUUID();

        CodeFileDocument document = CodeFileDocument.builder()
            .id(documentId)
            .repositoryId(repositoryId)
            .repositoryIdentifier("test-owner/test-repo")
            .filePath("src/main/java/Main.java")
            .fileName("Main.java")
            .fileExtension("java")
            .language("Java")
            .content("public class Main { }")
            .size(24L)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // When
        indexingService.indexCodeFile(document);

        // Then
        GetRequest getRequest = GetRequest.of(g -> g
            .index(FILES_INDEX_NAME)
            .id(documentId.toString())
        );

        GetResponse<CodeFileDocument> response = openSearchClient.get(getRequest, CodeFileDocument.class);

        assertThat(response.found()).isTrue();
        CodeFileDocument source = response.source();
        assertThat(source).isNotNull();
        assertThat(source.getId()).isEqualTo(documentId);
        assertThat(source.getRepositoryId()).isEqualTo(repositoryId.toString());
        assertThat(source.getRepositoryIdentifier()).isEqualTo("test-owner/test-repo");
        assertThat(source.getFilePath()).isEqualTo("src/main/java/Main.java");
        assertThat(source.getFileName()).isEqualTo("Main.java");
        assertThat(source.getFileExtension()).isEqualTo("java");
        assertThat(source.getLanguage()).isEqualTo("Java");
        assertThat(source.getContent()).isEqualTo("public class Main { }");
        assertThat(source.getSize().intValue()).isEqualTo(24);
    }

    @Test
    void testIndexCodeFile_WithChunks_ShouldIndexWithChunks() throws Exception {
        // Given
        var repositoryId = UUID.randomUUID();
        var documentId = UUID.randomUUID();

        List<CodeFileDocument.CodeChunk> codeChunks = new ArrayList<>();
        codeChunks.add(CodeFileDocument.CodeChunk.builder()
            .index(0)
            .content("chunk 1 content")
            .startLine(1)
            .endLine(10)
            .build());
        codeChunks.add(CodeFileDocument.CodeChunk.builder()
            .index(1)
            .content("chunk 2 content")
            .startLine(11)
            .endLine(20)
            .build());

        CodeFileDocument document = CodeFileDocument.builder()
            .id(documentId)
            .repositoryId(repositoryId)
            .repositoryIdentifier("test-owner/test-repo")
            .filePath("src/main/java/LargeFile.java")
            .fileName("LargeFile.java")
            .fileExtension("java")
            .language("Java")
            .content("large file content")
            .size(1000L)
            .codeChunks(codeChunks)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // When
        indexingService.indexCodeFile(document);

        // Wait for indexing
        

        // Then
        GetRequest getRequest = GetRequest.of(g -> g
            .index(FILES_INDEX_NAME)
            .id(documentId.toString())
        );

        GetResponse<CodeFileDocument> response = openSearchClient.get(getRequest, CodeFileDocument.class);

        assertThat(response.found()).isTrue();
        CodeFileDocument source = response.source();
        assertThat(source).isNotNull();

        List<CodeFileDocument.CodeChunk> indexedCodeChunks = source.getCodeChunks();
        assertThat(indexedCodeChunks).isNotNull();
        assertThat(indexedCodeChunks).hasSize(2);

        assertThat(indexedCodeChunks.get(0).getIndex()).isEqualTo(0);
        assertThat(indexedCodeChunks.get(0).getContent()).isEqualTo("chunk 1 content");
        assertThat(indexedCodeChunks.get(0).getStartLine()).isEqualTo(1);
        assertThat(indexedCodeChunks.get(0).getEndLine()).isEqualTo(10);

        assertThat(indexedCodeChunks.get(1).getIndex()).isEqualTo(1);
        assertThat(indexedCodeChunks.get(1).getContent()).isEqualTo("chunk 2 content");
    }

    @Test
    void testBulkIndexCodeFiles_ShouldIndexMultipleDocuments() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repoIdentifier = "test-owner/bulk-repo";
        List<CodeFileDocument> documents = generateTestDocuments(100, repositoryId, repoIdentifier);

        // When
        indexingService.bulkIndexCodeFiles(documents);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field(FieldNames.REPOSITORY_IDENTIFIER)
                    .value(FieldValue.of(repoIdentifier))
                )
            )
        );

        SearchResponse<CodeFileDocument> searchResponse = openSearchClient.search(searchRequest, CodeFileDocument.class);

        assertThat(searchResponse).isNotNull();
        assertThat(searchResponse.hits()).isNotNull();
        assertThat(searchResponse.hits().total()).isNotNull();
        assertThat(searchResponse.hits().total().value()).isEqualTo(100);
    }

    @Test
    void testBulkIndexCodeFiles_EmptyList_ShouldNotThrowException() {
        // Given
        List<CodeFileDocument> emptyList = new ArrayList<>();

        // When & Then
        assertThatCode(() -> indexingService.bulkIndexCodeFiles(emptyList)).doesNotThrowAnyException();
    }

    @Test
    void testBulkIndexCodeFiles_LargeBatch_ShouldHandleSuccessfully() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repoIdentifier = "test-owner/large-batch-repo";
        List<CodeFileDocument> documents = generateTestDocuments(1000, repositoryId, repoIdentifier);

        // When
        indexingService.bulkIndexCodeFiles(documents);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field(FieldNames.REPOSITORY_IDENTIFIER)
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(1100)
        );

        SearchResponse<CodeFileDocument> searchResponse = openSearchClient.search(searchRequest, CodeFileDocument.class);

        assertThat(searchResponse).isNotNull();
        assertThat(searchResponse.hits()).isNotNull();
        assertThat(searchResponse.hits().total()).isNotNull();
        assertThat(searchResponse.hits().total().value()).isEqualTo(1000);
    }

    @Test
    void testBulkIndexCodeFiles_MultipleLanguages_ShouldIndexAll() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repoIdentifier = "test-owner/multi-lang-repo";
        List<CodeFileDocument> documents = generateTestDocuments(500, repositoryId, repoIdentifier);

        // When
        indexingService.bulkIndexCodeFiles(documents);

        // Then - Verify total documents
        SearchRequest allDocsRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field(FieldNames.REPOSITORY_IDENTIFIER)
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(600)
        );

        SearchResponse<CodeFileDocument> allDocsResponse = openSearchClient.search(allDocsRequest, CodeFileDocument.class);
        assertThat(allDocsResponse).isNotNull();
        assertThat(allDocsResponse.hits()).isNotNull();
        assertThat(allDocsResponse.hits().total()).isNotNull();
        assertThat(allDocsResponse.hits().total().value()).isEqualTo(500);

        // Verify we have documents in different languages
        SearchRequest javaRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .bool(b -> b
                    .must(m -> m.term(t -> t.field(FieldNames.REPOSITORY_IDENTIFIER).value(FieldValue.of(repoIdentifier))))
                    .must(m -> m.term(t -> t.field(FieldNames.LANGUAGE).value(FieldValue.of("Java"))))
                )
            )
        );

        SearchResponse<CodeFileDocument> javaResponse = openSearchClient.search(javaRequest, CodeFileDocument.class);
        assertThat(javaResponse).isNotNull();
        assertThat(javaResponse.hits()).isNotNull();
        assertThat(javaResponse.hits().total()).isNotNull();
        assertThat(javaResponse.hits().total().value()).isGreaterThan(0);
    }

    @Test
    void testBulkIndexCodeFiles_VeryLargeBatch_ShouldHandleSuccessfully() throws Exception {
        // Given - Generate 2000 documents to test handling of very large batches
        UUID repositoryId = UUID.randomUUID();
        String repoIdentifier = "test-owner/very-large-batch-repo";
        List<CodeFileDocument> documents = generateTestDocuments(2000, repositoryId, repoIdentifier);

        // When
        indexingService.bulkIndexCodeFiles(documents);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field(FieldNames.REPOSITORY_IDENTIFIER)
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(2100)
        );

        SearchResponse<CodeFileDocument> searchResponse = openSearchClient.search(searchRequest, CodeFileDocument.class);

        assertThat(searchResponse).isNotNull();
        assertThat(searchResponse.hits()).isNotNull();
        assertThat(searchResponse.hits().total()).isNotNull();
        assertThat(searchResponse.hits().total().value()).isEqualTo(2000);
    }

    @Test
    void testBulkIndexCodeFiles_WithVariedSizes_ShouldIndexAll() throws Exception {
        // Given - Generate documents with varied content sizes
        UUID repositoryId = UUID.randomUUID();
        String repoIdentifier = "test-owner/varied-sizes-repo";
        List<CodeFileDocument> documents = new ArrayList<>();

        // Small files
        for (int i = 0; i < 100; i++) {
            String content = "// Small file\nclass Small" + i + " {}";
            documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID())
                .repositoryId(repositoryId)
                .repositoryIdentifier(repoIdentifier)
                .filePath("src/small/Small" + i + ".java")
                .fileName("Small" + i + ".java")
                .fileExtension("java")
                .language("Java")
                .content(content)
                .size((long) content.length())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
        }

        // Large files with generated content
        documents.addAll(generateTestDocuments(100, repositoryId, repoIdentifier));

        // When
        indexingService.bulkIndexCodeFiles(documents);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field(FieldNames.REPOSITORY_IDENTIFIER)
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(250)
        );

        SearchResponse<CodeFileDocument> searchResponse = openSearchClient.search(searchRequest, CodeFileDocument.class);

        assertThat(searchResponse).isNotNull();
        assertThat(searchResponse.hits()).isNotNull();
        assertThat(searchResponse.hits().total()).isNotNull();
        assertThat(searchResponse.hits().total().value()).isEqualTo(200);
    }
}

