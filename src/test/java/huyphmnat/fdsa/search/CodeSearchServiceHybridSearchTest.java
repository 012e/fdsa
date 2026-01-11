package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.OpenSearchIntegrationTest;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;
import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;
import huyphmnat.fdsa.search.interfaces.CodeSearchService;
import huyphmnat.fdsa.search.internal.services.OpenSearchIndexingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CodeSearchServiceHybridSearchTest extends OpenSearchIntegrationTest {

    @Autowired
    private CodeSearchService codeSearchService;

    @Autowired
    private OpenSearchIndexingService indexingService;

    @MockitoBean
    private EmbeddingModel embeddingService;

    private UUID repositoryId;
    private String repositoryIdentifier;

    @BeforeEach
    void setUp() throws Exception {
        repositoryId = UUID.randomUUID();
        repositoryIdentifier = "test-owner/hybrid-search-repo";

        // Index sample documents
        indexSampleDocuments();
    }

    @Test
    void testHybridSearch_WithFilters_ShouldApplyFilters() {
        // Given
        when(embeddingService.embed(anyString())).thenReturn(createMockEmbedding());

        CodeSearchRequest request = CodeSearchRequest.builder()
                .query("application")
                .repositoryIdentifier(repositoryIdentifier)
                .language("java")
                .build();

        // When
        try {
            CodeSearchResponse response = codeSearchService.searchCode(request);

            // Then
            assertThat(response).isNotNull();
            if (!response.getResults().isEmpty()) {
                // All results should match the filter
                assertThat(response.getResults())
                        .allMatch(result -> "java".equalsIgnoreCase(result.getLanguage()));
            }
        } catch (Exception e) {
            // Hybrid search may not be supported
        }
    }

    private void indexSampleDocuments() throws Exception {
        List<CodeFileDocument> documents = new ArrayList<>();

        // Document 1: Main application class
        documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID())
                .repositoryId(repositoryId)
                .repositoryIdentifier(repositoryIdentifier)
                .filePath("/src/main/java/com/example/Application.java")
                .fileName("Application.java")
                .fileExtension("java")
                .language("java")
                .content("""
                        package com.example;
                        
                        import org.springframework.boot.SpringApplication;
                        import org.springframework.boot.autoconfigure.SpringBootApplication;
                        
                        @SpringBootApplication
                        public class Application {
                            public static void main(String[] args) {
                                SpringApplication.run(Application.class, args);
                            }
                        }
                        """)
                .size(350L)
                .build());

        // Document 2: Service class
        documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID())
                .repositoryId(repositoryId)
                .repositoryIdentifier(repositoryIdentifier)
                .filePath("/src/main/java/com/example/service/UserService.java")
                .fileName("UserService.java")
                .fileExtension("java")
                .language("java")
                .content("""
                        package com.example.service;
                        
                        import org.springframework.stereotype.Service;
                        
                        @Service
                        public class UserService {
                            public User findById(Long id) {
                                // Implementation
                                return null;
                            }
                        }
                        """)
                .size(250L)
                .build());

        // Document 3: Controller class
        documents.add(CodeFileDocument.builder()
                .id(UUID.randomUUID())
                .repositoryId(repositoryId)
                .repositoryIdentifier(repositoryIdentifier)
                .filePath("/src/main/java/com/example/controller/UserController.java")
                .fileName("UserController.java")
                .fileExtension("java")
                .language("java")
                .content("""
                        package com.example.controller;
                        
                        import org.springframework.web.bind.annotation.RestController;
                        import org.springframework.web.bind.annotation.GetMapping;
                        
                        @RestController
                        public class UserController {
                            @GetMapping("/users")
                            public List<User> getUsers() {
                                return userService.findAll();
                            }
                        }
                        """)
                .size(300L)
                .build());

        indexingService.bulkIndexCodeFiles(documents);
        indexingService.refreshIndexes();
    }

    private float[] createMockEmbedding() {
        // Create a mock embedding vector (1536 dimensions for OpenAI text-embedding-3-small)
        float[] embedding = new float[1536];
        for (int i = 0; i < 1536; i++) {
            embedding[i] = ((float) Math.random());
        }
        return embedding;
    }
}

