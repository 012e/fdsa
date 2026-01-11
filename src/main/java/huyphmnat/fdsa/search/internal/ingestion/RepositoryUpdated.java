package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.RepositoryUpdatedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.search.interfaces.RepositoryIngestionService;
import huyphmnat.fdsa.shared.GroupIdConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryUpdated {

    private final RepositoryIngestionService repositoryIngestionService;

    @KafkaListener(topics = RepositoryTopics.REPOSITORY_UPDATED, groupId = GroupIdConfiguration.GROUP_ID)
    public void handleRepositoryUpdated(RepositoryUpdatedEvent event) {
        log.info("Received RepositoryUpdatedEvent for repository: {} ({}), {} changed files",
            event.getIdentifier(), event.getRepositoryId(), event.getChangedFiles().size());

        try {
            repositoryIngestionService.ingestChangedFiles(
                event.getRepositoryId(),
                event.getIdentifier(),
                event.getChangedFiles()
            );
            log.info("Successfully ingested changed files for repository: {}", event.getIdentifier());
        } catch (Exception e) {
            log.error("Failed to ingest changed files for repository: {}", event.getIdentifier(), e);
            // In production, you might want to retry or send to a dead letter queue
        }
    }
}

