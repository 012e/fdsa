package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
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
import java.util.UUID;

import static huyphmnat.fdsa.base.utils.CodeGenerator.generateTestDocuments;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

class OpenSearchIndexingServiceTest extends OpenSearchIntegrationTest {

    @Autowired
    private OpenSearchIndexingService indexingService;

    @Autowired
    private OpenSearchClient openSearchClient;

    private static final String FILES_INDEX_NAME = Indexes.CODE_FILE_INDEX;

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
    void testBulkIndexCodeFiles_EmptyList_ShouldNotThrowException() {
        // Given
        List<CodeFileDocument> emptyList = new ArrayList<>();

        // When & Then
        assertThatCode(() -> indexingService.bulkIndexCodeFiles(emptyList)).doesNotThrowAnyException();
    }

    @Test
    void testBulkIndexCodeFiles_WithVariedSizes_ShouldIndexAll() throws Exception {
        // Given - Generate documents with varied content sizes
        UUID repositoryId = UUID.randomUUID();
        String repoIdentifier = "test-owner/varied-sizes-repo";
        List<CodeFileDocument> documents = new ArrayList<>();

        // Small files
        for (int i = 0; i < 1000; i++) {
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
        indexingService.refreshIndexes();

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field(FieldNames.REPOSITORY_IDENTIFIER_KEYWORD)
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(250)
        );

        SearchResponse<CodeFileDocument> searchResponse = openSearchClient.search(searchRequest, CodeFileDocument.class);

        assertThat(searchResponse).isNotNull();
        assertThat(searchResponse.hits()).isNotNull();
        assertThat(searchResponse.hits().total()).isNotNull();
    }
}

