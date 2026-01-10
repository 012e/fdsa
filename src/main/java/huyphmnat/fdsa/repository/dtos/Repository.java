package huyphmnat.fdsa.repository.dtos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class Repository {
    @JsonIgnore
    private String fileSystemPath;
    private String description;
    private String identifier;
    private String ownerId;
}
