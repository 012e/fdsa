package huyphmnat.fdsa.repository.dtos;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = FileEntry.class, name = "FILE"),
    @JsonSubTypes.Type(value = DirectoryEntry.class, name = "DIRECTORY")
})
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class Entry {
    private String path;
    private String name;
    private FileEntryType type;
}

