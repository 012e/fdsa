package huyphmnat.fdsa.search;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.repository.dtos.FileCreatedEvent;
import huyphmnat.fdsa.repository.dtos.FileDeletedEvent;
import huyphmnat.fdsa.repository.dtos.FileUpdatedEvent;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.search.interfaces.FileIngestionService;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
class FileIngestionKafkaListenerTest extends BaseIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoBean
    private FileIngestionService fileIngestionService;

    @Test
    void testHandleFileCreated_ShouldInvokeIndexFile() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/test-repo";
        String filePath = "src/main/java/NewFile.java";

        FileCreatedEvent event = FileCreatedEvent.builder()
            .repositoryId(repositoryId)
            .repositoryIdentifier(repositoryIdentifier)
            .filePath(filePath)
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.FILE_CREATED, event);

        // Then
        TimeUnit.SECONDS.sleep(3);

        verify(fileIngestionService, times(1))
            .indexFile(eq(repositoryId), eq(repositoryIdentifier), eq(filePath));
    }

    @Test
    void testHandleFileUpdated_ShouldInvokeIndexFile() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/test-repo";
        String filePath = "src/main/java/UpdatedFile.java";

        FileUpdatedEvent event = FileUpdatedEvent.builder()
            .repositoryId(repositoryId)
            .repositoryIdentifier(repositoryIdentifier)
            .filePath(filePath)
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.FILE_UPDATED, event);

        // Then
        TimeUnit.SECONDS.sleep(3);

        verify(fileIngestionService, times(1))
            .indexFile(eq(repositoryId), eq(repositoryIdentifier), eq(filePath));
    }

    @Test
    void testHandleFileDeleted_ShouldInvokeRemoveFile() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String filePath = "src/main/java/DeletedFile.java";

        FileDeletedEvent event = FileDeletedEvent.builder()
            .repositoryId(repositoryId)
            .filePath(filePath)
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.FILE_DELETED, event);

        // Then
        TimeUnit.SECONDS.sleep(5);  // Increased wait time

        verify(fileIngestionService, atLeastOnce())
            .removeFile(eq(repositoryId), eq(filePath));
    }

    @Test
    void testHandleFileCreated_MultipleEvents_ShouldProcessAll() throws Exception {
        // Given
        UUID repositoryId = UUID.randomUUID();
        String repositoryIdentifier = "test-owner/test-repo";

        FileCreatedEvent event1 = FileCreatedEvent.builder()
            .repositoryId(repositoryId)
            .repositoryIdentifier(repositoryIdentifier)
            .filePath("file1.java")
            .build();

        FileCreatedEvent event2 = FileCreatedEvent.builder()
            .repositoryId(repositoryId)
            .repositoryIdentifier(repositoryIdentifier)
            .filePath("file2.java")
            .build();

        FileCreatedEvent event3 = FileCreatedEvent.builder()
            .repositoryId(repositoryId)
            .repositoryIdentifier(repositoryIdentifier)
            .filePath("file3.java")
            .build();

        // When
        kafkaTemplate.send(RepositoryTopics.FILE_CREATED, event1);
        kafkaTemplate.send(RepositoryTopics.FILE_CREATED, event2);
        kafkaTemplate.send(RepositoryTopics.FILE_CREATED, event3);

        // Then
        TimeUnit.SECONDS.sleep(5);

        verify(fileIngestionService, times(1))
            .indexFile(eq(repositoryId), eq(repositoryIdentifier), eq("file1.java"));
        verify(fileIngestionService, times(1))
            .indexFile(eq(repositoryId), eq(repositoryIdentifier), eq("file2.java"));
        verify(fileIngestionService, times(1))
            .indexFile(eq(repositoryId), eq(repositoryIdentifier), eq("file3.java"));
    }
}
