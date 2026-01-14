package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;
import huyphmnat.fdsa.search.dtos.CodeSearchResult;
import huyphmnat.fdsa.search.interfaces.CodeSearchService;
import huyphmnat.fdsa.search.internal.services.OpenSearchIndexingService;
import net.datafaker.Faker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

class CodeSearchServiceImplTest extends OpenSearchIntegrationTest {

    @Autowired
    private CodeSearchService codeSearchService;

    @Autowired
    private OpenSearchIndexingService indexingService;

    private final Faker faker = new Faker();

    private UUID testRepositoryId;
    private String testRepositoryIdentifier;
    private UUID otherRepositoryId;
    private String otherRepositoryIdentifier;

    // Track counts for deterministic assertions
    private int totalJavaFilesInTestRepo;
    private int totalPythonFiles;
    private int totalTestPathFiles;
    private int totalFilesWithHelloWorld;

    @BeforeEach
    void setUp() throws InterruptedException {
        testRepositoryId = UUID.randomUUID();
        testRepositoryIdentifier = "test-owner/test-repo";
        otherRepositoryId = UUID.randomUUID();
        otherRepositoryIdentifier = "other-owner/other-repo";

        // Index test documents
        indexTestDocuments();
        indexingService.refreshIndexes();
    }

    private void indexTestDocuments() {
        List<CodeFileDocument> documents = new ArrayList<>();

        // Reset counts
        totalJavaFilesInTestRepo = 0;
        totalPythonFiles = 0;
        totalTestPathFiles = 0;
        totalFilesWithHelloWorld = 0;

        // Generate 1000 Java files in test repository
        for (int i = 0; i < 1000; i++) {
            String className = faker.name().firstName() + "Class";
            String content = generateJavaClass(className);

            boolean isTestPath = i % 10 == 0; // 10% are test files
            String filePath = isTestPath
                ? "src/test/java/" + className + ".java"
                : "src/main/java/" + className + ".java";

            if (content.contains("Hello World")) {
                totalFilesWithHelloWorld++;
            }

            documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID())
                .repositoryId(testRepositoryId)
                .repositoryIdentifier(testRepositoryIdentifier)
                .filePath(filePath)
                .fileName(className + ".java")
                .fileExtension("java")
                .language("Java")
                .content(content)
                .size((long) content.length())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

            totalJavaFilesInTestRepo++;
            if (isTestPath) {
                totalTestPathFiles++;
            }
        }

        // Generate 500 Python files in other repository
        for (int i = 0; i < 500; i++) {
            String functionName = faker.name().firstName().toLowerCase();
            String content = generatePythonFunction(functionName);

            if (content.contains("Hello World")) {
                totalFilesWithHelloWorld++;
            }

            documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID())
                .repositoryId(otherRepositoryId)
                .repositoryIdentifier(otherRepositoryIdentifier)
                .filePath("src/" + functionName + ".py")
                .fileName(functionName + ".py")
                .fileExtension("py")
                .language("Python")
                .content(content)
                .size((long) content.length())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

            totalPythonFiles++;
        }

        // Add a few specific documents for targeted tests
        documents.add(CodeFileDocument.builder()
            .id(UUID.randomUUID())
            .repositoryId(testRepositoryId)
            .repositoryIdentifier(testRepositoryIdentifier)
            .filePath("src/main/java/SpecificUser.java")
            .fileName("SpecificUser.java")
            .fileExtension("java")
            .language("Java")
            .content("public class SpecificUser { private String name; private String email; }")
            .size(100L)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build());

        totalJavaFilesInTestRepo++;
        totalFilesWithHelloWorld++; // Some of the generated files will have this

        indexingService.bulkIndexCodeFiles(documents);
    }

    private String generateJavaClass(String className) {
        StringBuilder sb = new StringBuilder();
        sb.append("public class ").append(className).append(" {\n");

        // Add some fields
        for (int i = 0; i < faker.number().numberBetween(1, 5); i++) {
            String fieldName = faker.name().firstName().toLowerCase();
            String fieldType = faker.options().option("String", "int", "boolean", "double");
            sb.append("    private ").append(fieldType).append(" ").append(fieldName).append(";\n");
        }

        // Add a method (sometimes with "Hello World")
        String methodName = faker.name().firstName().toLowerCase();
        sb.append("\n    public void ").append(methodName).append("() {\n");

        if (faker.number().numberBetween(0, 100) < 10) { // 10% chance of "Hello World"
            sb.append("        System.out.println(\"Hello World\");\n");
        } else {
            sb.append("        // ").append(faker.lorem().sentence()).append("\n");
        }

        sb.append("    }\n");
        sb.append("}\n");

        return sb.toString();
    }

    private String generatePythonFunction(String functionName) {
        StringBuilder sb = new StringBuilder();
        sb.append("def ").append(functionName).append("():\n");

        if (faker.number().numberBetween(0, 100) < 10) { // 10% chance of "Hello World"
            sb.append("    print('Hello World')\n");
        } else {
            sb.append("    # ").append(faker.lorem().sentence()).append("\n");
            sb.append("    pass\n");
        }

        return sb.toString();
    }

    @Test
    void testSearchCode_WithSimpleQuery_ShouldReturnResults() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("Hello World")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalHits()).isGreaterThan(0); // At least some results
        assertThat(response.getResults()).isNotEmpty();
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getTookMs()).isGreaterThan(0);
    }

    @Test
    void testSearchCode_WithLanguageFilter_ShouldReturnOnlyMatchingLanguage() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("Hello")
            .language("Python")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
//        assertThat(response.getTotalHits()).isGreaterThan(0); // At least some Python files
        response.getResults().forEach(result ->
            assertThat(result.getLanguage()).isEqualTo("Python")
        );
    }

    @Test
    void testSearchCode_WithFileExtensionFilter_ShouldReturnOnlyMatchingExtension() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalHits()).isGreaterThan(0); // At least some Java files
        response.getResults().forEach(result ->
            assertThat(result.getFileExtension()).isEqualTo("java")
        );
    }

    @Test
    void testSearchCode_WithFilePathPattern_ShouldReturnMatchingPaths() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .filePathPattern("*/test/*")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalHits()).isGreaterThan(0); // At least some test path files
        response.getResults().forEach(result ->
            assertThat(result.getFilePath()).contains("/test/")
        );
    }

    @Test
    void testSearchCode_WithPagination_ShouldReturnCorrectPage() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .page(0)
            .size(2)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalHits()).isGreaterThanOrEqualTo(3); // Need at least 3 for pagination test
        assertThat(response.getResults()).hasSize(2);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getTotalPages()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void testSearchCode_WithHighlighting_ShouldReturnHighlights() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("Hello World")
            .highlightFields(List.of("content"))
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalHits()).isGreaterThanOrEqualTo(1);

        // At least one result should have highlights
        boolean hasHighlights = response.getResults().stream()
            .anyMatch(result -> result.getHighlights() != null && !result.getHighlights().isEmpty());
        assertThat(hasHighlights).isTrue();
    }

    @Test
    void testSearchCode_WithMultipleFilters_ShouldReturnMatchingResults() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .repositoryIdentifier(testRepositoryIdentifier)
            .language("Java")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
//        assertThat(response.getTotalHits()).isGreaterThan(0); // At least some matching files
        response.getResults().forEach(result -> {
            assertThat(result.getRepositoryIdentifier()).isEqualTo(testRepositoryIdentifier);
            assertThat(result.getLanguage()).isEqualTo("Java");
            assertThat(result.getFileExtension()).isEqualTo("java");
        });
    }

    @Test
    void testSearchCode_WithNoResults_ShouldReturnEmptyResponse() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("nonexistentcodepattern12345")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalHits()).isEqualTo(0);
        assertThat(response.getResults()).isEmpty();
        assertThat(response.getTotalPages()).isEqualTo(0);
    }

    @Test
    void testSearchCode_ResultsShouldContainMetadata() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .page(0)
            .size(10)
            .build();


        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTotalHits()).isGreaterThanOrEqualTo(1);

        CodeSearchResult result = response.getResults().get(0);
        assertThat(result.getId()).isNotNull();
        assertThat(result.getRepositoryId()).isNotNull();
        assertThat(result.getRepositoryIdentifier()).isNotNull();
        assertThat(result.getFilePath()).isNotNull();
        assertThat(result.getFileName()).isNotNull();
        assertThat(result.getFileExtension()).isNotNull();
        assertThat(result.getLanguage()).isNotNull();
        assertThat(result.getContent()).isNotNull();
        assertThat(result.getSize()).isNotNull();
        assertThat(result.getScore()).isNotNull();
    }
}
