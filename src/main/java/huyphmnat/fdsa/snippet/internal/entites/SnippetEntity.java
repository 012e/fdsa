package huyphmnat.fdsa.snippet.internal.entites;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.id.uuid.UuidVersion7Strategy;

import java.util.UUID;

@Getter
@Setter
@Table(name = "snippets", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"owner", "path"})
})
@Entity
public class SnippetEntity {
    @Id
    @UuidGenerator(algorithm = UuidVersion7Strategy.class)
    private UUID id;

    @Column(nullable = false)
    @NotEmpty()
    private String owner;

    @Column(nullable = false)
    @NotEmpty()
    private String path;

    @Column(nullable = false)
    @NotEmpty()
    private String code;
}
