package huyphmnat.fdsa.snippet.internal.entites;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.id.uuid.UuidVersion7Strategy;

import java.util.UUID;

@Getter
@Setter
@Table(name = "snippets")
@Entity
public class SnippetEntity {
    @Id
    @UuidGenerator(algorithm = UuidVersion7Strategy.class)
    private UUID id;

    @Column(nullable = false, unique = true)
    @NotEmpty()
    private String path;

    @Column(nullable = false)
    @NotEmpty()
    private String code;
}
