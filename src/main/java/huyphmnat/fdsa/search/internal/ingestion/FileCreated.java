package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.FileCreatedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.search.interfaces.FileIngestionService;
import huyphmnat.fdsa.shared.GroupIdConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileCreated {

    private final FileIngestionService fileIngestionService;

    @KafkaListener(topics = RepositoryTopics.FILE_CREATED, groupId = GroupIdConfiguration.GROUP_ID)
    public void handleFileCreated(FileCreatedEvent event, Acknowledgment acknowledgment) {
        log.info("Received FileCreatedEvent for file: {} in repository: {}",
            event.getFilePath(), event.getRepositoryIdentifier());

        try {
            fileIngestionService.indexFile(
                event.getRepositoryId(),
                event.getRepositoryIdentifier(),
                event.getFilePath()
            );
            log.info("Successfully indexed new file: {}", event.getFilePath());
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.debug("Acknowledged message for file: {}", event.getFilePath());
            }
        } catch (Exception e) {
            log.error("Failed to index new file: {}", event.getFilePath(), e);
            // Message will not be acknowledged, will be reprocessed
        }
    }
}
