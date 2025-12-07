package huyphmnat.fdsa.snippet.dtos;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class SnippetCreatedEvent {
    private UUID id;
    private String code;
}
