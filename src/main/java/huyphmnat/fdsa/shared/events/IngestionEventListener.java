package huyphmnat.fdsa.shared.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import huyphmnat.fdsa.repository.dtos.RepositoryClonedEvent;
import huyphmnat.fdsa.shared.ingestion.RepositoryIngestionService;
import huyphmnat.fdsa.snippet.dtos.SnippetCreatedEvent;
import huyphmnat.fdsa.shared.ingestion.SnippetIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class IngestionEventListener {

    private final SnippetIngestionService snippetIngestionService;
    private final RepositoryIngestionService repositoryIngestionService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "snippet.created", groupId = "ingestion-service")
    public void handleSnippetCreated(String message) {
        try {
            log.info("Received snippet.created event: {}", message);
            SnippetCreatedEvent event = objectMapper.readValue(message, SnippetCreatedEvent.class);

            String snippetId = event.getId().toString();
            String code = event.getCode();

            snippetIngestionService.ingestSnippet(snippetId, code);
        } catch (Exception e) {
            log.error("Failed to process snippet.created event", e);
        }
    }

    @KafkaListener(topics = "repository.cloned", groupId = "ingestion-service")
    public void handleRepositoryCloned(String message) {
        try {
            log.info("Received repository.cloned event: {}", message);
            RepositoryClonedEvent event = objectMapper.readValue(message, RepositoryClonedEvent.class);

            repositoryIngestionService.ingestRepository(event.getId(), event.getIdentifier());
        } catch (Exception e) {
            log.error("Failed to process repository.cloned event", e);
        }
    }
}

