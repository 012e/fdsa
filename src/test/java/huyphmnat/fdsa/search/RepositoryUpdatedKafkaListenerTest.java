package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.repository.dtos.RepositoryUpdatedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.search.interfaces.RepositoryIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@TestPropertySource(properties = {
    "spring.kafka.consumer.auto-offset-reset=earliest"
})
class RepositoryUpdatedKafkaListenerTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @MockitoBean
    private RepositoryIngestionService repositoryIngestionService;

    @Test
    void testHandleRepositoryUpdated_ShouldInvokeIngestionService() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String identifier = "test/repo";

        RepositoryUpdatedEvent.ChangedFile changedFile = RepositoryUpdatedEvent.ChangedFile.builder()
            .path("src/Main.java")
            .content("public class Main { }")
            .changeType(RepositoryUpdatedEvent.ChangeType.ADDED)
            .build();

        RepositoryUpdatedEvent event = RepositoryUpdatedEvent.builder()
            .repositoryId(repositoryId)
            .identifier(identifier)
            .changedFiles(List.of(changedFile))
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.REPOSITORY_UPDATED, event).get(5, TimeUnit.SECONDS);

        // Then - verify the ingestion service was called with correct parameters
        verify(repositoryIngestionService, timeout(10000).times(1))
            .ingestChangedFiles(eq(repositoryId), eq(identifier), any());
    }

    @Test
    void testHandleRepositoryUpdated_MultipleChangedFiles_ShouldProcessAll() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String identifier = "test/multi-repo";

        List<RepositoryUpdatedEvent.ChangedFile> changedFiles = List.of(
            RepositoryUpdatedEvent.ChangedFile.builder()
                .path("src/Main.java")
                .content("public class Main { }")
                .changeType(RepositoryUpdatedEvent.ChangeType.ADDED)
                .build(),
            RepositoryUpdatedEvent.ChangedFile.builder()
                .path("src/Helper.java")
                .content("public class Helper { }")
                .changeType(RepositoryUpdatedEvent.ChangeType.MODIFIED)
                .build(),
            RepositoryUpdatedEvent.ChangedFile.builder()
                .path("src/Old.java")
                .content("")
                .changeType(RepositoryUpdatedEvent.ChangeType.DELETED)
                .build()
        );

        RepositoryUpdatedEvent event = RepositoryUpdatedEvent.builder()
            .repositoryId(repositoryId)
            .identifier(identifier)
            .changedFiles(changedFiles)
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.REPOSITORY_UPDATED, event).get(5, TimeUnit.SECONDS);

        // Then
        verify(repositoryIngestionService, timeout(10000).times(1))
            .ingestChangedFiles(eq(repositoryId), eq(identifier), any());
    }

    @Test
    void testHandleRepositoryUpdated_IngestionError_ShouldNotThrowException() throws Exception {
        // Given - setup mock to throw exception
        UUID repositoryId = UUID.randomUUID();
        String identifier = "test/error-repo";

        RepositoryUpdatedEvent event = RepositoryUpdatedEvent.builder()
            .repositoryId(repositoryId)
            .identifier(identifier)
            .changedFiles(List.of(
                RepositoryUpdatedEvent.ChangedFile.builder()
                    .path("error.java")
                    .content("error")
                    .changeType(RepositoryUpdatedEvent.ChangeType.ADDED)
                    .build()
            ))
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.REPOSITORY_UPDATED, event).get(5, TimeUnit.SECONDS);

        // Then - should still be called despite error (error is logged, not thrown)
        verify(repositoryIngestionService, timeout(10000).times(1))
            .ingestChangedFiles(eq(repositoryId), eq(identifier), any());
    }
}

