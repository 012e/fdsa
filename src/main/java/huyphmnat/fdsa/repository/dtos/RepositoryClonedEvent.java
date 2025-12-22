package huyphmnat.fdsa.repository.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RepositoryClonedEvent {
    private UUID id;
    private String identifier;
    private String sourceUrl;
    private String filesystemPath;
}

