package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.RepositoryClonedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.search.interfaces.RepositoryIngestionService;
import huyphmnat.fdsa.shared.GroupIdConfiguration;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RepositoryCloned {

    private final RepositoryIngestionService repositoryIngestionService;

    @KafkaListener(topics = RepositoryTopics.REPOSITORY_CLONED, groupId = GroupIdConfiguration.GROUP_ID)
    @Observed(name = "repository.cloned.event.handling", contextualName = "Handle RepositoryClonedEvent")
    public void handleRepositoryCloned(RepositoryClonedEvent event, Acknowledgment acknowledgment) {
        log.info("Received RepositoryClonedEvent for repository: {} ({})",
            event.getIdentifier(), event.getId());

        try {
            repositoryIngestionService.ingestRepository(event.getId(), event.getIdentifier());
            log.info("Successfully ingested repository: {}", event.getIdentifier());
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.debug("Acknowledged message for repository: {}", event.getIdentifier());
            }
        } catch (Exception e) {
            log.error("Failed to ingest repository: {}", event.getIdentifier(), e);
            // Message will not be acknowledged, will be reprocessed
        }
    }
}
