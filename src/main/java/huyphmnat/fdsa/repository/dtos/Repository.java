package huyphmnat.fdsa.repository.dtos;

import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Repository {
    private String filesystemPath;
    private String description;
    private String identifier;
}
