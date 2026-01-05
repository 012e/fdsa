package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.search.internal.models.CodeFileDocument;
import huyphmnat.fdsa.search.internal.services.OpenSearchIndexingService;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

class OpenSearchIndexingServiceTest extends OpenSearchIntegrationTest {

    @Autowired
    private OpenSearchIndexingService indexingService;

    @Autowired
    private OpenSearchClient openSearchClient;

    private static final String FILES_INDEX_NAME = "code_files";

    @Test
    void testIndexCodeFile_ShouldIndexSuccessfully() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String documentId = UUID.randomUUID().toString();

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

        // Wait for indexing
        Thread.sleep(1000);

        // Then
        GetRequest getRequest = GetRequest.of(g -> g
            .index(FILES_INDEX_NAME)
            .id(documentId)
        );

        GetResponse<Map> response = openSearchClient.get(getRequest, Map.class);

        assertThat(response.found()).isTrue();
        Map<String, Object> source = response.source();
        assertThat(source).isNotNull();
        assertThat(source.get("id")).isEqualTo(documentId);
        assertThat(source.get("repository_id")).isEqualTo(repositoryId.toString());
        assertThat(source.get("repository_identifier")).isEqualTo("test-owner/test-repo");
        assertThat(source.get("file_path")).isEqualTo("src/main/java/Main.java");
        assertThat(source.get("file_name")).isEqualTo("Main.java");
        assertThat(source.get("file_extension")).isEqualTo("java");
        assertThat(source.get("language")).isEqualTo("Java");
        assertThat(source.get("content")).isEqualTo("public class Main { }");
        assertThat(((Number) source.get("size")).intValue()).isEqualTo(24);
    }

    @Test
    void testIndexCodeFile_WithChunks_ShouldIndexWithChunks() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String documentId = UUID.randomUUID().toString();

        List<CodeFileDocument.CodeChunk> chunks = new ArrayList<>();
        chunks.add(CodeFileDocument.CodeChunk.builder()
            .index(0)
            .content("chunk 1 content")
            .startLine(1)
            .endLine(10)
            .build());
        chunks.add(CodeFileDocument.CodeChunk.builder()
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
            .chunks(chunks)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        // When
        indexingService.indexCodeFile(document);

        // Wait for indexing
        Thread.sleep(1000);

        // Then
        GetRequest getRequest = GetRequest.of(g -> g
            .index(FILES_INDEX_NAME)
            .id(documentId)
        );

        GetResponse<Map> response = openSearchClient.get(getRequest, Map.class);

        assertThat(response.found()).isTrue();
        Map<String, Object> source = response.source();
        assertThat(source).isNotNull();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> indexedChunks = (List<Map<String, Object>>) source.get("chunks");
        assertThat(indexedChunks).isNotNull();
        assertThat(indexedChunks).hasSize(2);

        assertThat(indexedChunks.get(0).get("index")).isEqualTo(0);
        assertThat(indexedChunks.get(0).get("content")).isEqualTo("chunk 1 content");
        assertThat(indexedChunks.get(0).get("start_line")).isEqualTo(1);
        assertThat(indexedChunks.get(0).get("end_line")).isEqualTo(10);

        assertThat(indexedChunks.get(1).get("index")).isEqualTo(1);
        assertThat(indexedChunks.get(1).get("content")).isEqualTo("chunk 2 content");
    }

    @Test
    void testBulkIndexCodeFiles_ShouldIndexMultipleDocuments() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        List<CodeFileDocument> documents = new ArrayList<>();

        for (int i = 0; i < 5; i++) {
            documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(repositoryId)
                .repositoryIdentifier("test-owner/bulk-repo")
                .filePath("src/file" + i + ".java")
                .fileName("file" + i + ".java")
                .fileExtension("java")
                .language("Java")
                .content("content " + i)
                .size((long) (10 + i))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
        }

        // When
        indexingService.bulkIndexCodeFiles(documents);

        // Wait for indexing
        Thread.sleep(1000);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_identifier")
                    .value(FieldValue.of("test-owner/bulk-repo"))
                )
            )
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertThat(searchResponse.hits().total().value()).isEqualTo(5);
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
        List<CodeFileDocument> documents = new ArrayList<>();

        for (int i = 0; i < 150; i++) {
            documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(repositoryId)
                .repositoryIdentifier("test-owner/large-batch-repo")
                .filePath("src/file" + i + ".java")
                .fileName("file" + i + ".java")
                .fileExtension("java")
                .language("Java")
                .content("content " + i)
                .size((long) (10 + i))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
        }

        // When
        indexingService.bulkIndexCodeFiles(documents);

        // Wait for indexing
        Thread.sleep(2000);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_identifier")
                    .value(FieldValue.of("test-owner/large-batch-repo"))
                )
            )
            .size(200)
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertThat(searchResponse.hits().total().value()).isEqualTo(150);
    }
}

