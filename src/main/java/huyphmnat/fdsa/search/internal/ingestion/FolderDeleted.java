package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.FolderDeletedEvent;
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
public class FolderDeleted {

    private final FileIngestionService fileIngestionService;

    @KafkaListener(topics = RepositoryTopics.FOLDER_DELETED, groupId = GroupIdConfiguration.GROUP_ID)
    public void handleFolderDeleted(FolderDeletedEvent event) {
        log.info("Received FolderDeletedEvent for folder: {} in repository: {}",
            event.getFolderPath(), event.getRepositoryIdentifier());

        try {
            // Remove all files within the deleted folder from the index
            fileIngestionService.removeFolder(
                event.getRepositoryId(),
                event.getFolderPath()
            );
            log.info("Successfully removed all files from deleted folder: {}", event.getFolderPath());
        } catch (Exception e) {
            log.error("Failed to remove files from deleted folder: {}", event.getFolderPath(), e);
            // In production, you might want to retry or send to a dead letter queue
        }
    }
}
