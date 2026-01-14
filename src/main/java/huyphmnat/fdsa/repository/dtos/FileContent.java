package huyphmnat.fdsa.repository.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class FileContent {
    private String path;
    private String name;
    private Long size;
    private String content;
}

