package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.repository.dtos.*;
import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import huyphmnat.fdsa.search.interfaces.RepositoryIngestionService;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RepositoryIndexingServiceImplTest extends OpenSearchIntegrationTest {

    @Autowired
    private RepositoryIngestionService ingestionService;

    @MockitoBean
    private RepositoryFileService repositoryFileService;

    @Autowired
    private OpenSearchClient openSearchClient;

    private static final String FILES_INDEX_NAME = "code_files";

    @Test
    void testIngestRepository_SimpleStructure_ShouldIndexAllFiles() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/simple-repo";

        // Mock root directory
        DirectoryContent rootContent = DirectoryContent.builder()
            .path("/")
            .entries(List.of(
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/Main.java")
                    .name("Main.java")
                    .size(100L)
                    .build(),
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/README.md")
                    .name("README.md")
                    .size(50L)
                    .build()
            ))
            .build();

        when(repositoryFileService.listDirectory(eq(repositoryId), eq("/")))
            .thenReturn(rootContent);

        when(repositoryFileService.readFile(eq(repositoryId), eq("/Main.java")))
            .thenReturn(FileContent.builder()
                .path("/Main.java")
                .name("Main.java")
                .content("public class Main { }".getBytes(StandardCharsets.UTF_8))
                .size(100L)
                .build());

        when(repositoryFileService.readFile(eq(repositoryId), eq("/README.md")))
            .thenReturn(FileContent.builder()
                .path("/README.md")
                .name("README.md")
                .content("# Test".getBytes(StandardCharsets.UTF_8))
                .size(50L)
                .build());

        // When
        ingestionService.ingestRepository(repositoryId, repositoryIdentifier);

        // Wait for indexing
        Thread.sleep(1500);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_id")
                    .value(FieldValue.of(repositoryId.toString()))
                )
            )
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertEquals(2, searchResponse.hits().total().value(), "Should index 2 files");

        verify(repositoryFileService, times(1)).listDirectory(repositoryId, "/");
        verify(repositoryFileService, times(1)).readFile(repositoryId, "/Main.java");
        verify(repositoryFileService, times(1)).readFile(repositoryId, "/README.md");
    }

    @Test
    void testIngestRepository_NestedStructure_ShouldTraverseDirectories() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/nested-repo";

        // Mock root directory with subdirectory
        DirectoryContent rootContent = DirectoryContent.builder()
            .path("/")
            .entries(List.of(
                DirectoryEntry.builder()
                    .type(FileEntryType.DIRECTORY)
                    .path("/src")
                    .name("src")
                    .build(),
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/README.md")
                    .name("README.md")
                    .size(50L)
                    .build()
            ))
            .build();

        // Mock /src directory
        DirectoryContent srcContent = DirectoryContent.builder()
            .path("/src")
            .entries(List.of(
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/src/Main.java")
                    .name("Main.java")
                    .size(100L)
                    .build(),
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/src/Helper.java")
                    .name("Helper.java")
                    .size(80L)
                    .build()
            ))
            .build();

        when(repositoryFileService.listDirectory(eq(repositoryId), eq("/")))
            .thenReturn(rootContent);
        when(repositoryFileService.listDirectory(eq(repositoryId), eq("/src")))
            .thenReturn(srcContent);

        when(repositoryFileService.readFile(eq(repositoryId), eq("/README.md")))
            .thenReturn(FileContent.builder()
                .path("/README.md")
                .name("README.md")
                .content("# Test".getBytes(StandardCharsets.UTF_8))
                .size(50L)
                .build());

        when(repositoryFileService.readFile(eq(repositoryId), eq("/src/Main.java")))
            .thenReturn(FileContent.builder()
                .path("/src/Main.java")
                .name("Main.java")
                .content("public class Main { }".getBytes(StandardCharsets.UTF_8))
                .size(100L)
                .build());

        when(repositoryFileService.readFile(eq(repositoryId), eq("/src/Helper.java")))
            .thenReturn(FileContent.builder()
                .path("/src/Helper.java")
                .name("Helper.java")
                .content("public class Helper { }".getBytes(StandardCharsets.UTF_8))
                .size(80L)
                .build());

        // When
        ingestionService.ingestRepository(repositoryId, repositoryIdentifier);

        // Wait for indexing
        Thread.sleep(1500);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_id")
                    .value(FieldValue.of(repositoryId.toString()))
                )
            )
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertEquals(3, searchResponse.hits().total().value(), "Should index 3 files");

        verify(repositoryFileService, times(1)).listDirectory(repositoryId, "/");
        verify(repositoryFileService, times(1)).listDirectory(repositoryId, "/src");
    }

    @Test
    void testIngestRepository_SkipsNonCodeFiles() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/mixed-repo";

        DirectoryContent rootContent = DirectoryContent.builder()
            .path("/")
            .entries(List.of(
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/Main.java")
                    .name("Main.java")
                    .size(100L)
                    .build(),
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/image.png")
                    .name("image.png")
                    .size(5000L)
                    .build(),
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/document.pdf")
                    .name("document.pdf")
                    .size(10000L)
                    .build()
            ))
            .build();

        when(repositoryFileService.listDirectory(eq(repositoryId), eq("/")))
            .thenReturn(rootContent);

        when(repositoryFileService.readFile(eq(repositoryId), eq("/Main.java")))
            .thenReturn(FileContent.builder()
                .path("/Main.java")
                .name("Main.java")
                .content("public class Main { }".getBytes(StandardCharsets.UTF_8))
                .size(100L)
                .build());

        // When
        ingestionService.ingestRepository(repositoryId, repositoryIdentifier);

        // Wait for indexing
        Thread.sleep(1500);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_id")
                    .value(FieldValue.of(repositoryId.toString()))
                )
            )
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertEquals(1, searchResponse.hits().total().value(), "Should only index code files");

        // Should not attempt to read non-code files
        verify(repositoryFileService, never()).readFile(repositoryId, "/image.png");
        verify(repositoryFileService, never()).readFile(repositoryId, "/document.pdf");
    }

    @Test
    void testIngestRepository_SkipsLargeFiles() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/large-file-repo";

        DirectoryContent rootContent = DirectoryContent.builder()
            .path("/")
            .entries(List.of(
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/Small.java")
                    .name("Small.java")
                    .size(100L)
                    .build(),
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/VeryLarge.java")
                    .name("VeryLarge.java")
                    .size(15L * 1024 * 1024) // 15MB - exceeds 10MB limit
                    .build()
            ))
            .build();

        when(repositoryFileService.listDirectory(eq(repositoryId), eq("/")))
            .thenReturn(rootContent);

        when(repositoryFileService.readFile(eq(repositoryId), eq("/Small.java")))
            .thenReturn(FileContent.builder()
                .path("/Small.java")
                .name("Small.java")
                .content("public class Small { }".getBytes(StandardCharsets.UTF_8))
                .size(100L)
                .build());

        // When
        ingestionService.ingestRepository(repositoryId, repositoryIdentifier);

        // Wait for indexing
        Thread.sleep(1500);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_id")
                    .value(FieldValue.of(repositoryId.toString()))
                )
            )
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertEquals(1, searchResponse.hits().total().value(), "Should skip large files");

        // Should not attempt to read large file
        verify(repositoryFileService, never()).readFile(repositoryId, "/VeryLarge.java");
    }

    @Test
    void testIngestRepository_ChunksLargeFiles() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/chunking-repo";

        // Create large content (>100KB to trigger chunking)
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 5000; i++) {
            largeContent.append("public class Class").append(i).append(" {\n");
            largeContent.append("    private String field;\n");
            largeContent.append("    public void method() { }\n");
            largeContent.append("}\n\n");
        }

        DirectoryContent rootContent = DirectoryContent.builder()
            .path("/")
            .entries(List.of(
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/LargeFile.java")
                    .name("LargeFile.java")
                    .size((long) largeContent.length())
                    .build()
            ))
            .build();

        when(repositoryFileService.listDirectory(eq(repositoryId), eq("/")))
            .thenReturn(rootContent);

        when(repositoryFileService.readFile(eq(repositoryId), eq("/LargeFile.java")))
            .thenReturn(FileContent.builder()
                .path("/LargeFile.java")
                .name("LargeFile.java")
                .content(largeContent.toString().getBytes(StandardCharsets.UTF_8))
                .size((long) largeContent.length())
                .build());

        // When
        ingestionService.ingestRepository(repositoryId, repositoryIdentifier);

        // Wait for indexing
        Thread.sleep(1500);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_id")
                    .value(FieldValue.of(repositoryId.toString()))
                )
            )
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertEquals(1, searchResponse.hits().total().value());

        // Verify chunks were created
        Map<String, Object> document = searchResponse.hits().hits().get(0).source();
        assertNotNull(document.get("chunks"), "Large file should have chunks");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> chunks = (List<Map<String, Object>>) document.get("chunks");
        assertTrue(chunks.size() > 1, "Large file should be split into multiple chunks");
    }

    @Test
    void testIngestRepository_ContinuesOnFileError() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/error-repo";

        DirectoryContent rootContent = DirectoryContent.builder()
            .path("/")
            .entries(List.of(
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/Good.java")
                    .name("Good.java")
                    .size(100L)
                    .build(),
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/Bad.java")
                    .name("Bad.java")
                    .size(100L)
                    .build(),
                FileEntry.builder()
                    .type(FileEntryType.FILE)
                    .path("/AlsoGood.java")
                    .name("AlsoGood.java")
                    .size(100L)
                    .build()
            ))
            .build();

        when(repositoryFileService.listDirectory(eq(repositoryId), eq("/")))
            .thenReturn(rootContent);

        when(repositoryFileService.readFile(eq(repositoryId), eq("/Good.java")))
            .thenReturn(FileContent.builder()
                .path("/Good.java")
                .name("Good.java")
                .content("public class Good { }".getBytes(StandardCharsets.UTF_8))
                .size(100L)
                .build());

        // Simulate error reading this file
        when(repositoryFileService.readFile(eq(repositoryId), eq("/Bad.java")))
            .thenThrow(new RuntimeException("Failed to read file"));

        when(repositoryFileService.readFile(eq(repositoryId), eq("/AlsoGood.java")))
            .thenReturn(FileContent.builder()
                .path("/AlsoGood.java")
                .name("AlsoGood.java")
                .content("public class AlsoGood { }".getBytes(StandardCharsets.UTF_8))
                .size(100L)
                .build());

        // When
        ingestionService.ingestRepository(repositoryId, repositoryIdentifier);

        // Wait for indexing
        Thread.sleep(1500);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_id")
                    .value(FieldValue.of(repositoryId.toString()))
                )
            )
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        // Should index the successful files
        assertEquals(2, searchResponse.hits().total().value(),
            "Should index successful files despite error on one file");
    }

    @Test
    void testIngestRepository_BulkIndexing() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/bulk-repo";

        // Create 150 files to test bulk indexing
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 150; i++) {
            entries.add(FileEntry.builder()
                .type(FileEntryType.FILE)
                .path("/File" + i + ".java")
                .name("File" + i + ".java")
                .size(100L)
                .build());
        }

        DirectoryContent rootContent = DirectoryContent.builder()
            .path("/")
            .entries(entries)
            .build();

        when(repositoryFileService.listDirectory(eq(repositoryId), eq("/")))
            .thenReturn(rootContent);

        // Mock file reading for all files
        for (int i = 0; i < 150; i++) {
            String path = "/File" + i + ".java";
            when(repositoryFileService.readFile(eq(repositoryId), eq(path)))
                .thenReturn(FileContent.builder()
                    .path(path)
                    .name("File" + i + ".java")
                    .content(("public class File" + i + " { }").getBytes(StandardCharsets.UTF_8))
                    .size(100L)
                    .build());
        }

        // When
        ingestionService.ingestRepository(repositoryId, repositoryIdentifier);

        // Wait for indexing
        Thread.sleep(2000);

        // Then
        SearchRequest searchRequest = SearchRequest.of(s -> s
            .index(FILES_INDEX_NAME)
            .query(q -> q
                .term(t -> t
                    .field("repository_id")
                    .value(FieldValue.of(repositoryId.toString()))
                )
            )
            .size(200)
        );

        SearchResponse<Map> searchResponse = openSearchClient.search(searchRequest, Map.class);

        assertEquals(150, searchResponse.hits().total().value(), "Should index all 150 files");
    }
}

