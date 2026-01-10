package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.search.internal.models.CodeFileDocument;
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
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.*;

class OpenSearchIndexingServiceTest extends OpenSearchIntegrationTest {

    @Autowired
    private OpenSearchIndexingService indexingService;

    @Autowired
    private OpenSearchClient openSearchClient;

    private final Faker faker = new Faker();

    private static final String FILES_INDEX_NAME = Indexes.CODE_FILE_INDEX;

    private String generateJavaClass(String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(className).append(" {\n");

        // Add fields
        for (int i = 0; i < faker.number().numberBetween(2, 6); i++) {
            String fieldName = faker.name().firstName().toLowerCase();
            String fieldType = faker.options().option("String", "int", "boolean", "double", "long");
            sb.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
        }

        sb.append("\n");

        // Add constructor
        sb.append("    public ").append(className).append("() {\n");
        sb.append("        // Constructor\n");
        sb.append("    }\n\n");

        // Add methods
        for (int i = 0; i < faker.number().numberBetween(1, 4); i++) {
            String methodName = faker.name().firstName().toLowerCase();
            sb.append("    public void ").append(methodName).append("() {\n");
            sb.append("        // ").append(faker.lorem().sentence()).append("\n");
            sb.append("    }\n\n");
        }

        sb.append("}\n");
        return sb.toString();
    }

    private String generatePythonFunction(String functionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("def ").append(functionName).append("(");

        // Add parameters
        int paramCount = faker.number().numberBetween(0, 4);
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append(faker.name().firstName().toLowerCase());
        }

        sb.append("):\n");
        sb.append("    \"\"\"").append(faker.lorem().sentence()).append("\"\"\"\n");

        // Add some logic
        for (int i = 0; i < faker.number().numberBetween(1, 3); i++) {
            sb.append("    # ").append(faker.lorem().sentence()).append("\n");
        }
        sb.append("    pass\n");

        return sb.toString();
    }

    private String generateJavaScriptFunction(String functionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("function ").append(functionName).append("(");

        int paramCount = faker.number().numberBetween(0, 3);
        for (int i = 0; i < paramCount; i++) {
            if (i > 0) sb.append(", ");
            sb.append(faker.name().firstName().toLowerCase());
        }

        sb.append(") {\n");
        sb.append("  // ").append(faker.lorem().sentence()).append("\n");
        sb.append("  return null;\n");
        sb.append("}\n");

        return sb.toString();
    }

    private List<CodeFileDocument> generateTestDocuments(int count, UUID repositoryId, String repositoryIdentifier) {
        List<CodeFileDocument> documents = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String language;
            String extension;
            String content;
            String fileName;

            // Randomly choose language
            int langChoice = faker.number().numberBetween(0, 3);
            if (langChoice == 0) {
                language = "Java";
                extension = "java";
                String className = faker.name().firstName() + "Class";
                fileName = className + ".java";
                content = generateJavaClass(className);
            } else if (langChoice == 1) {
                language = "Python";
                extension = "py";
                String funcName = faker.name().firstName().toLowerCase();
                fileName = funcName + ".py";
                content = generatePythonFunction(funcName);
            } else {
                language = "JavaScript";
                extension = "js";
                String funcName = faker.name().firstName().toLowerCase();
                fileName = funcName + ".js";
                content = generateJavaScriptFunction(funcName);
            }

            String filePath = "src/" + (i % 10 == 0 ? "test/" : "main/") + fileName;

            documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(repositoryId)
                .repositoryIdentifier(repositoryIdentifier)
                .filePath(filePath)
                .fileName(fileName)
                .fileExtension(extension)
                .language(language)
                .content(content)
                .size((long) content.length())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
        }

        return documents;
    }

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

        // Then
        GetRequest getRequest = GetRequest.of(g -> g
            .index(FILES_INDEX_NAME)
            .id(documentId)
        );

        GetResponse<Map<String, Object>> response = openSearchClient.get(getRequest, Map.class);

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
        

        // Then
        GetRequest getRequest = GetRequest.of(g -> g
            .index(FILES_INDEX_NAME)
            .id(documentId)
        );

        GetResponse<Map<String, Object>> response = openSearchClient.get(getRequest, Map.class);

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
        String repoIdentifier = "test-owner/bulk-repo";
        List<CodeFileDocument> documents = generateTestDocuments(100, repositoryId, repoIdentifier);

        // When
        indexingService.bulkIndexCodeFiles(documents);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_identifier")
                    .value(FieldValue.of(repoIdentifier))
                )
            )
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

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
                    .field("repository_identifier")
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(1100)
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

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
                    .field("repository_identifier")
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(600)
        );

        SearchResponse<Map> allDocsResponse = openSearchClient.search(allDocsRequest, Map.class);
        assertThat(allDocsResponse.hits().total().value()).isEqualTo(500);

        // Verify we have documents in different languages
        SearchRequest javaRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .bool(b -> b
                    .must(m -> m.term(t -> t.field("repository_identifier").value(FieldValue.of(repoIdentifier))))
                    .must(m -> m.term(t -> t.field("language").value(FieldValue.of("Java"))))
                )
            )
        );

        SearchResponse<Map> javaResponse = openSearchClient.search(javaRequest, Map.class);
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
                    .field("repository_identifier")
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(2100)
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

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
                .id(UUID.randomUUID().toString())
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
                    .field("repository_identifier")
                    .value(FieldValue.of(repoIdentifier))
                )
            )
            .size(250)
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertThat(searchResponse.hits().total().value()).isEqualTo(200);
    }
}

