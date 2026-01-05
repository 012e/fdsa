package huyphmnat.fdsa.search.internal.ingestion;

import huyphmnat.fdsa.repository.dtos.RepositoryClonedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.shared.GroupIdConfiguration;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RepositoryCloned {
    @KafkaListener(topics = RepositoryTopics.REPOSITORY_CLONED, groupId = GroupIdConfiguration.GROUP_ID)
    public void handleRepositoryUpdated(RepositoryClonedEvent repository) {
    }
}
