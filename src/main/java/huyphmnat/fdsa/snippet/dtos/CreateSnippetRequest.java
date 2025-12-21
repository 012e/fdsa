package huyphmnat.fdsa.snippet.dtos;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateSnippetRequest {
    private String owner;
    private String path;
    private String code;
}
