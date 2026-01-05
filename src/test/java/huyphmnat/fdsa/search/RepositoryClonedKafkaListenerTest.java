package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.repository.dtos.RepositoryClonedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.search.interfaces.RepositoryIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RepositoryClonedKafkaListenerTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private RepositoryIngestionService ingestionService;

    @Test
    void testHandleRepositoryCloned_ShouldInvokeIngestionService() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/test-repo";

        RepositoryClonedEvent event = RepositoryClonedEvent.builder()
            .id(repositoryId)
            .identifier(repositoryIdentifier)
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.REPOSITORY_CLONED, event);

        // Then
        // Wait for Kafka consumer to process the message
        TimeUnit.SECONDS.sleep(3);

        verify(ingestionService, times(1))
            .ingestRepository(eq(repositoryId), eq(repositoryIdentifier));
    }

    @Test
    void testHandleRepositoryCloned_MultipleEvents_ShouldProcessAll() throws Exception {
        // Given
        UUID repo1Id = UUID.randomUUID();
        UUID repo2Id = UUID.randomUUID();
        UUID repo3Id = UUID.randomUUID();

        RepositoryClonedEvent event1 = RepositoryClonedEvent.builder()
            .id(repo1Id)
            .identifier("owner1/repo1")
            .build();

        RepositoryClonedEvent event2 = RepositoryClonedEvent.builder()
            .id(repo2Id)
            .identifier("owner2/repo2")
            .build();

        RepositoryClonedEvent event3 = RepositoryClonedEvent.builder()
            .id(repo3Id)
            .identifier("owner3/repo3")
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.REPOSITORY_CLONED, event1);
        kafkaTemplate.send(RepositoryTopics.REPOSITORY_CLONED, event2);
        kafkaTemplate.send(RepositoryTopics.REPOSITORY_CLONED, event3);

        // Then
        // Wait for Kafka consumer to process all messages
        TimeUnit.SECONDS.sleep(5);

        verify(ingestionService, times(1)).ingestRepository(eq(repo1Id), eq("owner1/repo1"));
        verify(ingestionService, times(1)).ingestRepository(eq(repo2Id), eq("owner2/repo2"));
        verify(ingestionService, times(1)).ingestRepository(eq(repo3Id), eq("owner3/repo3"));
    }

    @Test
    void testHandleRepositoryCloned_IngestionError_ShouldNotThrowException() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/error-repo";

        RepositoryClonedEvent event = RepositoryClonedEvent.builder()
            .id(repositoryId)
            .identifier(repositoryIdentifier)
            .build();

        // Simulate ingestion service throwing an exception
        doThrow(new RuntimeException("Ingestion failed"))
            .when(ingestionService)
            .ingestRepository(eq(repositoryId), eq(repositoryIdentifier));

        // When
        kafkaTemplate.send(RepositoryTopics.REPOSITORY_CLONED, event);

        // Then
        // Wait for Kafka consumer to process the message
        TimeUnit.SECONDS.sleep(3);

        // Verify the service was called despite the error
        verify(ingestionService, times(1))
            .ingestRepository(eq(repositoryId), eq(repositoryIdentifier));

        // The listener should catch the exception and log it, not propagate it
        // This test passes if no exception is thrown
    }
}

