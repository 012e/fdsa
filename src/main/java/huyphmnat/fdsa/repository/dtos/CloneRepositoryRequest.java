package huyphmnat.fdsa.repository.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloneRepositoryRequest {
    private String sourceUrl;
    private String identifier;
    private String description;
}

