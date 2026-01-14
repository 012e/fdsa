package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.FileDeletedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.search.interfaces.FileIngestionService;
import huyphmnat.fdsa.shared.GroupIdConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FileDeleted {

    private final FileIngestionService fileIngestionService;

    @KafkaListener(topics = RepositoryTopics.FILE_DELETED, groupId = GroupIdConfiguration.GROUP_ID)
    public void handleFileDeleted(FileDeletedEvent event) {
        log.info("Received FileDeletedEvent for file: {} in repository: {}",
            event.getFilePath(), event.getRepositoryIdentifier());

        try {
            fileIngestionService.removeFile(
                event.getRepositoryId(),
                event.getFilePath()
            );
            log.info("Successfully removed deleted file from index: {}", event.getFilePath());
        } catch (Exception e) {
            log.error("Failed to remove deleted file from index: {}", event.getFilePath(), e);
            // In production, you might want to retry or send to a dead letter queue
        }
    }
}
