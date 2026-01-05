package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;
import huyphmnat.fdsa.search.dtos.CodeSearchResult;
import huyphmnat.fdsa.search.interfaces.CodeSearchService;
import huyphmnat.fdsa.search.internal.models.CodeFileDocument;
import huyphmnat.fdsa.search.internal.services.OpenSearchIndexingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CodeSearchServiceImplTest extends OpenSearchIntegrationTest {

    @Autowired
    private CodeSearchService codeSearchService;

    @Autowired
    private OpenSearchIndexingService indexingService;

    private UUID testRepositoryId;
    private String testRepositoryIdentifier;

    @BeforeEach
    void setUp() throws InterruptedException {
        testRepositoryId = UUID.randomUUID();
        testRepositoryIdentifier = "test-owner/test-repo";

        // Index test documents
        indexTestDocuments();

        // Wait for indexing to complete
        Thread.sleep(1500);
    }

    private void indexTestDocuments() {
        List<CodeFileDocument> documents = List.of(
            CodeFileDocument.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(testRepositoryId)
                .repositoryIdentifier(testRepositoryIdentifier)
                .filePath("src/main/java/Main.java")
                .fileName("Main.java")
                .fileExtension("java")
                .language("Java")
                .content("public class Main { public static void main(String[] args) { System.out.println(\"Hello World\"); } }")
                .size(100L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build(),
            
            CodeFileDocument.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(testRepositoryId)
                .repositoryIdentifier(testRepositoryIdentifier)
                .filePath("src/main/java/User.java")
                .fileName("User.java")
                .fileExtension("java")
                .language("Java")
                .content("public class User { private String name; private String email; public User(String name, String email) { this.name = name; this.email = email; } }")
                .size(150L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build(),
            
            CodeFileDocument.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(UUID.randomUUID())
                .repositoryIdentifier("other-owner/other-repo")
                .filePath("src/app.py")
                .fileName("app.py")
                .fileExtension("py")
                .language("Python")
                .content("def hello(): print('Hello World')")
                .size(50L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build(),
            
            CodeFileDocument.builder()
                .id(UUID.randomUUID().toString())
                .repositoryId(testRepositoryId)
                .repositoryIdentifier(testRepositoryIdentifier)
                .filePath("src/test/java/MainTest.java")
                .fileName("MainTest.java")
                .fileExtension("java")
                .language("Java")
                .content("public class MainTest { @Test public void testMain() { assertTrue(true); } }")
                .size(80L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()
        );

        indexingService.bulkIndexCodeFiles(documents);
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
        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 2, "Should find at least 2 documents with 'Hello World'");
        assertFalse(response.getResults().isEmpty());
        assertEquals(0, response.getPage());
        assertTrue(response.getTookMs() > 0);
    }

    @Test
    void testSearchCode_WithRepositoryFilter_ShouldReturnOnlyMatchingRepository() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .repositoryId(testRepositoryId)
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 3, "Should find at least 3 Java files in test repository");
        response.getResults().forEach(result -> 
            assertEquals(testRepositoryId, result.getRepositoryId())
        );
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
        assertNotNull(response);
        assertEquals(1, response.getTotalHits(), "Should find only 1 Python file");
        assertEquals("Python", response.getResults().get(0).getLanguage());
    }

    @Test
    void testSearchCode_WithFileExtensionFilter_ShouldReturnOnlyMatchingExtension() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .fileExtension("java")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 3, "Should find at least 3 Java files");
        response.getResults().forEach(result -> 
            assertEquals("java", result.getFileExtension())
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
        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 1, "Should find test files");
        response.getResults().forEach(result -> 
            assertTrue(result.getFilePath().contains("/test/"))
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
        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 3);
        assertEquals(2, response.getResults().size(), "Should return exactly 2 results");
        assertEquals(0, response.getPage());
        assertTrue(response.getTotalPages() >= 2);
    }

    @Test
    void testSearchCode_WithHighlighting_ShouldReturnHighlights() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("Hello World")
            .highlightFields(Arrays.asList("content"))
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 1);
        
        // At least one result should have highlights
        boolean hasHighlights = response.getResults().stream()
            .anyMatch(result -> result.getHighlights() != null && !result.getHighlights().isEmpty());
        assertTrue(hasHighlights, "At least one result should have highlights");
    }

    @Test
    void testSearchCode_WithMultipleFilters_ShouldReturnMatchingResults() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .repositoryIdentifier(testRepositoryIdentifier)
            .language("Java")
            .fileExtension("java")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 3);
        response.getResults().forEach(result -> {
            assertEquals(testRepositoryIdentifier, result.getRepositoryIdentifier());
            assertEquals("Java", result.getLanguage());
            assertEquals("java", result.getFileExtension());
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
        assertNotNull(response);
        assertEquals(0, response.getTotalHits());
        assertTrue(response.getResults().isEmpty());
        assertEquals(0, response.getTotalPages());
    }

    @Test
    void testSearchCode_ResultsShouldContainMetadata() {
        // Given
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("User")
            .page(0)
            .size(10)
            .build();

        // When
        CodeSearchResponse response = codeSearchService.searchCode(request);

        // Then
        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 1);
        
        CodeSearchResult result = response.getResults().get(0);
        assertNotNull(result.getId());
        assertNotNull(result.getRepositoryId());
        assertNotNull(result.getRepositoryIdentifier());
        assertNotNull(result.getFilePath());
        assertNotNull(result.getFileName());
        assertNotNull(result.getFileExtension());
        assertNotNull(result.getLanguage());
        assertNotNull(result.getContent());
        assertNotNull(result.getSize());
        assertNotNull(result.getScore());
        assertTrue(result.getScore() > 0, "Score should be greater than 0");
    }
}

