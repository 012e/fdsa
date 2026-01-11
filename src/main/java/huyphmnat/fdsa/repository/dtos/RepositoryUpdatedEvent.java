package huyphmnat.fdsa.repository.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryUpdatedEvent {
    private UUID repositoryId;
    private String identifier;
    private List<ChangedFile> changedFiles;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ChangedFile {
        private String path;
        private String content;
        private ChangeType changeType;
    }

    public enum ChangeType {
        ADDED,
        MODIFIED,
        DELETED
    }
}

