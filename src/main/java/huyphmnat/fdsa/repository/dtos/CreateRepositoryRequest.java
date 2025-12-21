package huyphmnat.fdsa.repository.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRepositoryRequest {
    private String identifier;
    private String owner;
    private String name;
    private String description;
}

