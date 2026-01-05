package huyphmnat.fdsa.shared.ingestion;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class OpenAIService {

    private final RestClient restClient;
    private final String model;
    private final String embeddingModel;

    public OpenAIService(
            @Value("${openai.api.key}") String apiKey,
            @Value("${openai.mt odel:gpt-4o-mini}") String model,
            @Value("${openai.embedding.model:text-embedding-3-large}") String embeddingModel,
            @Value("${openai.api.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.model = model;
        this.embeddingModel = embeddingModel;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String summarizeCode(String code) {
        log.info("Summarizing code...");
        var request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a code summarization assistant. Analyze the provided code and create a concise, " +
                                        "informative summary that describes what the code does, its main purpose, key components, " +
                                        "and functionality. Focus on the high-level behavior and intent rather than implementation details. " +
                                        "Keep the summary clear and professional."),
                        Map.of("role", "user", "content", "Please summarize this code:\n\n" + code)
                )
        );

        var response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(Map.class);

        return extractContent(response);
    }

    public String summarizeChunk(String chunk, int index) {
        log.info("Summarizing chunk {}...", index);
        var request = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a code summarization assistant. Analyze the provided code snippet and create a " +
                                        "brief, informative summary. Focus on what this specific portion of code does and its purpose. " +
                                        "Keep it concise but meaningful."),
                        Map.of("role", "user", "content",
                                "Please summarize this code chunk (part " + (index + 1) + "):\n\n" + chunk)
                )
        );

        var response = restClient.post()
                .uri("/chat/completions")
                .body(request)
                .retrieve()
                .body(Map.class);

        return extractContent(response);
    }

    public List<Double> embedText(String text) {
        log.info("Generating embedding for text...");
        var request = Map.of(
                "model", embeddingModel,
                "input", text,
                "dimensions", 1024
        );

        var response = restClient.post()
                .uri("/embeddings")
                .body(request)
                .retrieve()
                .body(Map.class);

        return extractEmbedding(response);
    }

    public List<List<Double>> embedTexts(List<String> texts) {
        log.info("Generating embeddings for {} texts...", texts.size());
        var request = Map.of(
                "model", embeddingModel,
                "input", texts,
                "dimensions", 1024
        );

        var response = restClient.post()
                .uri("/embeddings")
                .body(request)
                .retrieve()
                .body(Map.class);

        return extractEmbeddings(response);
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        var choices = (List<Map<String, Object>>) response.get("choices");
        var message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    @SuppressWarnings("unchecked")
    private List<Double> extractEmbedding(Map<String, Object> response) {
        var data = (List<Map<String, Object>>) response.get("data");
        return (List<Double>) data.get(0).get("embedding");
    }

    @SuppressWarnings("unchecked")
    private List<List<Double>> extractEmbeddings(Map<String, Object> response) {
        var data = (List<Map<String, Object>>) response.get("data");
        return data.stream()
                .map(item -> (List<Double>) item.get("embedding"))
                .toList();
    }
}

