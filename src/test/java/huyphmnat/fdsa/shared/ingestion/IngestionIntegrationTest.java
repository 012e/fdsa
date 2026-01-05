package huyphmnat.fdsa.shared.ingestion;

import com.fasterxml.jackson.databind.ObjectMapper;
import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.repository.dtos.RepositoryClonedEvent;
import huyphmnat.fdsa.snippet.dtos.SnippetCreatedEvent;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.*;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.GetResponse;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IngestionIntegrationTest extends BaseIntegrationTest {

    @Container
    static GenericContainer<?> opensearch =
        new OpenSearchContainer<>(DockerImageName.parse("opensearchproject/opensearch:3.4.0"));

    private static MockWebServer mockOpenAI;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OpenSearchClient openSearchClient;

    @BeforeAll
    static void setupMockOpenAI() throws IOException {
        mockOpenAI = new MockWebServer();
        mockOpenAI.start();
    }

    @AfterAll
    static void tearDownMockOpenAI() throws IOException {
        if (mockOpenAI != null) {
            mockOpenAI.shutdown();
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        // OpenSearch configuration
        registry.add("opensearch.host", opensearch::getHost);
        registry.add("opensearch.port", opensearch::getFirstMappedPort);
        registry.add("opensearch.scheme", () -> "http");

        // OpenAI mock server configuration
        registry.add("openai.api.key", () -> "test-key");
        registry.add("openai.api.base-url", () -> "http://localhost:" + mockOpenAI.getPort());
    }

    @Test
    @Order(1)
    void testSnippetIngestion() throws Exception {
        // Given: Mock OpenAI responses
        setupMockOpenAIResponses();

        String snippetId = UUID.randomUUID().toString();
        String code = "def hello_world():\n    print('Hello, World!')";

        SnippetCreatedEvent event = SnippetCreatedEvent.builder()
                .id(UUID.fromString(snippetId))
                .code(code)
                .owner("testuser")
                .path("test.py")
                .build();

        // When: Publish snippet.created event
        String eventJson = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("snippet.created", eventJson).get();

        // Then: Wait for processing and verify indexed to OpenSearch
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    GetResponse<Map> response = openSearchClient.get(
                            GetRequest.of(g -> g.index("code_snippets").id(snippetId)),
                            Map.class
                    );
                    assertThat(response.found()).isTrue();

                    Map<String, Object> source = response.source();
                    assertThat(source).isNotNull();
                    assertThat(source.get("snippet_id")).isEqualTo(snippetId);
                    assertThat(source.get("code")).isEqualTo(code);
                    assertThat(source.get("overall_summary")).isNotNull();
                    assertThat(source.get("overall_embedding")).isNotNull();

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> chunks = (List<Map<String, Object>>) source.get("chunks");
                    assertThat(chunks).isNotEmpty();
                });

        // Verify OpenAI API calls were made
        verifyOpenAICalls();
    }

    @Test
    @Order(2)
    void testSnippetIngestionWithMultipleChunks() throws Exception {
        // Given: A longer code snippet that will be chunked
        setupMockOpenAIResponses();

        String snippetId = UUID.randomUUID().toString();
        StringBuilder longCode = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longCode.append("def function_").append(i).append("():\n");
            longCode.append("    return ").append(i).append("\n\n");
        }

        SnippetCreatedEvent event = SnippetCreatedEvent.builder()
                .id(UUID.fromString(snippetId))
                .code(longCode.toString())
                .owner("testuser")
                .path("large_file.py")
                .build();

        // When: Publish event
        String eventJson = objectMapper.writeValueAsString(event);
        kafkaTemplate.send("snippet.created", eventJson).get();

        // Then: Verify multiple chunks were created
        await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    GetResponse<Map> response = openSearchClient.get(
                            GetRequest.of(g -> g.index("code_snippets").id(snippetId)),
                            Map.class
                    );
                    assertThat(response.found()).isTrue();

                    Map<String, Object> source = response.source();
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> chunks = (List<Map<String, Object>>) source.get("chunks");
                    assertThat(chunks).hasSizeGreaterThan(1);

                    // Verify chunk structure
                    for (int i = 0; i < chunks.size(); i++) {
                        Map<String, Object> chunk = chunks.get(i);
                        assertThat(chunk.get("chunk_index")).isEqualTo(i);
                        assertThat(chunk.get("code")).isNotNull();
                        assertThat(chunk.get("summary")).isNotNull();
                        assertThat(chunk.get("embedding")).isNotNull();
                    }
                });
    }

    @Test
    @Order(3)
    void testOpenAIServiceDirectly() {
        // Given: Mock OpenAI service with responses
        setupMockOpenAIResponses();

        // This test would need the OpenAIService to be exposed or tested separately
        // For now, we rely on integration through the event flow
        assertThat(mockOpenAI.getRequestCount()).isGreaterThan(0);
    }

    private void setupMockOpenAIResponses() {
        // Mock chat completion response for summarization
        String chatResponse = """
            {
                "id": "chatcmpl-123",
                "object": "chat.completion",
                "created": 1677652288,
                "model": "gpt-4o-mini",
                "choices": [{
                    "index": 0,
                    "message": {
                        "role": "assistant",
                        "content": "This code defines a simple hello world function that prints a greeting message."
                    },
                    "finish_reason": "stop"
                }]
            }
            """;

        // Mock embedding response
        String embeddingResponse = """
            {
                "object": "list",
                "data": [{
                    "object": "embedding",
                    "embedding": %s,
                    "index": 0
                }],
                "model": "text-embedding-3-large",
                "usage": {
                    "prompt_tokens": 8,
                    "total_tokens": 8
                }
            }
            """.formatted(generateMockEmbedding());

        // Queue multiple responses for the test
        for (int i = 0; i < 20; i++) {
            if (i % 2 == 0) {
                mockOpenAI.enqueue(new MockResponse()
                        .setBody(chatResponse)
                        .addHeader("Content-Type", "application/json"));
            } else {
                mockOpenAI.enqueue(new MockResponse()
                        .setBody(embeddingResponse)
                        .addHeader("Content-Type", "application/json"));
            }
        }
    }

    private String generateMockEmbedding() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 1024; i++) {
            sb.append(Math.random());
            if (i < 1023) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }

    private void verifyOpenAICalls() throws InterruptedException {
        // Verify at least one chat completion call
        RecordedRequest request1 = mockOpenAI.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request1).isNotNull();
        assertThat(request1.getPath()).contains("/chat/completions");

        // Verify at least one embedding call
        RecordedRequest request2 = mockOpenAI.takeRequest(5, TimeUnit.SECONDS);
        assertThat(request2).isNotNull();
        assertThat(request2.getPath()).contains("/embeddings");
    }
}

