package huyphmnat.fdsa.snippet.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SnippetCreatedEvent {
    private UUID id;
    private String path;
    private String code;
}
