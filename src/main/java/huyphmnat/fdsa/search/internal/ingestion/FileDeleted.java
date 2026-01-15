package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.FileDeletedEvent;
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
public class FileDeleted {

    private final FileIngestionService fileIngestionService;

    @KafkaListener(topics = RepositoryTopics.FILE_DELETED, groupId = GroupIdConfiguration.GROUP_ID)
    public void handleFileDeleted(FileDeletedEvent event, Acknowledgment acknowledgment) {
        log.info("Received FileDeletedEvent for file: {} in repository: {}",
            event.getFilePath(), event.getRepositoryIdentifier());

        try {
            fileIngestionService.removeFile(
                event.getRepositoryId(),
                event.getFilePath()
            );
            log.info("Successfully removed deleted file from index: {}", event.getFilePath());
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
                log.debug("Acknowledged message for file: {}", event.getFilePath());
            }
        } catch (Exception e) {
            log.error("Failed to remove deleted file from index: {}", event.getFilePath(), e);
            // Message will not be acknowledged, will be reprocessed
        }
    }
}
