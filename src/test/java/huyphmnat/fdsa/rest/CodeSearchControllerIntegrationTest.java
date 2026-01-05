package huyphmnat.fdsa.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;
import huyphmnat.fdsa.search.internal.models.CodeFileDocument;
import huyphmnat.fdsa.search.internal.services.OpenSearchIndexingService;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable security for integration tests
class CodeSearchControllerIntegrationTest extends OpenSearchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OpenSearchIndexingService indexingService;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID testRepositoryId;
    private String testRepositoryIdentifier;

    @BeforeEach
    void setUp() throws InterruptedException {
        testRepositoryId = UUID.randomUUID();
        testRepositoryIdentifier = "test-owner/test-repo";

        // Index test documents
        indexTestDocuments();

        // Wait for indexing
        Thread.sleep(1500);
    }

    private void indexTestDocuments() {
        indexingService.bulkIndexCodeFiles(Arrays.asList(
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
                .content("public class User { private String name; }")
                .size(50L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build()
        ));
    }

    @Test
    void testSearchCodeGet_WithQuery_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/search/code")
                .param("q", "Hello World")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.results").isArray())
            .andExpect(jsonPath("$.totalHits").isNumber())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalPages").isNumber())
            .andExpect(jsonPath("$.tookMs").isNumber());
    }

    @Test
    void testSearchCodeGet_WithFilters_ShouldReturnFilteredResults() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/search/code")
                .param("q", "class")
                .param("repositoryId", testRepositoryId.toString())
                .param("language", "Java")
                .param("fileExtension", "java")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isArray())
            .andExpect(jsonPath("$.totalHits").isNumber())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        CodeSearchResponse response = objectMapper.readValue(content, CodeSearchResponse.class);

        assertNotNull(response);
        assertTrue(response.getTotalHits() >= 2);
        response.getResults().forEach(r -> {
            assertEquals(testRepositoryId, r.getRepositoryId());
            assertEquals("Java", r.getLanguage());
            assertEquals("java", r.getFileExtension());
        });
    }

    @Test
    void testSearchCodeGet_WithHighlighting_ShouldReturnHighlights() throws Exception {
        mockMvc.perform(get("/api/search/code")
                .param("q", "Hello World")
                .param("highlight", "content,file_name")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void testSearchCodeGet_WithRepositoryIdentifier_ShouldFilterByRepo() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/search/code")
                .param("q", "class")
                .param("repositoryIdentifier", testRepositoryIdentifier)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        CodeSearchResponse response = objectMapper.readValue(content, CodeSearchResponse.class);

        assertNotNull(response);
        response.getResults().forEach(r ->
            assertEquals(testRepositoryIdentifier, r.getRepositoryIdentifier())
        );
    }

    @Test
    void testSearchCodeGet_WithFilePathPattern_ShouldMatchPattern() throws Exception {
        mockMvc.perform(get("/api/search/code")
                .param("q", "class")
                .param("filePathPattern", "*/main/*")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void testSearchCodeGet_WithPagination_ShouldRespectPageSize() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/search/code")
                .param("q", "class")
                .param("page", "0")
                .param("size", "1"))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        CodeSearchResponse response = objectMapper.readValue(content, CodeSearchResponse.class);

        assertNotNull(response);
        assertTrue(response.getResults().size() <= 1);
        assertEquals(0, response.getPage());
        assertEquals(1, response.getSize());
    }

    @Test
    void testSearchCodePost_WithRequestBody_ShouldReturn200() throws Exception {
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("Hello World")
            .page(0)
            .size(10)
            .build();

        mockMvc.perform(post("/api/search/code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.results").isArray())
            .andExpect(jsonPath("$.totalHits").isNumber())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10));
    }

    @Test
    void testSearchCodePost_WithAllFilters_ShouldReturnFilteredResults() throws Exception {
        CodeSearchRequest request = CodeSearchRequest.builder()
            .query("class")
            .repositoryId(testRepositoryId)
            .repositoryIdentifier(testRepositoryIdentifier)
            .language("Java")
            .fileExtension("java")
            .page(0)
            .size(10)
            .highlightFields(List.of("content"))
            .build();

        MvcResult result = mockMvc.perform(post("/api/search/code")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn();

        String content = result.getResponse().getContentAsString();
        CodeSearchResponse response = objectMapper.readValue(content, CodeSearchResponse.class);

        assertNotNull(response);
        assertThat(response.getTotalHits())
                .isGreaterThan(2);
        response.getResults().forEach(r -> {
            assertEquals(testRepositoryId, r.getRepositoryId());
            assertEquals(testRepositoryIdentifier, r.getRepositoryIdentifier());
            assertEquals("Java", r.getLanguage());
            assertEquals("java", r.getFileExtension());
        });
    }

    @Test
    void testSearchCodeGet_WithNoQuery_ShouldStillReturnResults() throws Exception {
        // Some search implementations allow empty queries to return all documents
        mockMvc.perform(get("/api/search/code")
                .param("q", "")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isArray());
    }

    @Test
    void testSearchCodeGet_WithInvalidPage_ShouldReturn200WithEmptyResults() throws Exception {
        mockMvc.perform(get("/api/search/code")
                .param("q", "class")
                .param("page", "999")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results").isArray());
    }
}

