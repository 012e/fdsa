package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.FolderCreatedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.shared.GroupIdConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class FolderCreated {

    @KafkaListener(topics = RepositoryTopics.FOLDER_CREATED, groupId = GroupIdConfiguration.GROUP_ID)
    public void handleFolderCreated(FolderCreatedEvent event) {
        log.info("Received FolderCreatedEvent for folder: {} in repository: {}",
            event.getFolderPath(), event.getRepositoryIdentifier());

        // Note: Folder creation doesn't require indexing action since folders are just containers.
        // Files added to the folder will trigger separate FileCreatedEvents.
        log.debug("No indexing action needed for folder creation: {}", event.getFolderPath());
    }
}
