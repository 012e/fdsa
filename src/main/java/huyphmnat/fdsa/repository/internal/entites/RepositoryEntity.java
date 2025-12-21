package huyphmnat.fdsa.repository.internal.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "repositories")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepositoryEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String identifier;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private String name;

    @Column
    private String description;

    @Column(nullable = false)
    private String filesystemPath;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;
}

