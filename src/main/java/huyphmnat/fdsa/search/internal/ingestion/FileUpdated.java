package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.FileUpdatedEvent;
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
public class FileUpdated {

    private final FileIngestionService fileIngestionService;

    @KafkaListener(topics = RepositoryTopics.FILE_UPDATED, groupId = GroupIdConfiguration.GROUP_ID)
    public void handleFileUpdated(FileUpdatedEvent event, Acknowledgment acknowledgment) {
        log.info("Received FileUpdatedEvent for file: {} in repository: {}",
            event.getFilePath(), event.getRepositoryIdentifier());

        try {
            // Re-index the file (this will replace the old version)
            fileIngestionService.indexFile(
                event.getRepositoryId(),
                event.getRepositoryIdentifier(),
                event.getFilePath()
            );
            log.info("Successfully re-indexed updated file: {}", event.getFilePath());
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.debug("Acknowledged message for file: {}", event.getFilePath());
            }
        } catch (Exception e) {
            log.error("Failed to re-index updated file: {}", event.getFilePath(), e);
            // Message will not be acknowledged, will be reprocessed
        }
    }
}
