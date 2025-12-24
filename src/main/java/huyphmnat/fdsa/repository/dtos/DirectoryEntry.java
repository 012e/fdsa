package huyphmnat.fdsa.repository.dtos;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DirectoryEntry extends Entry {
    // Directories don't have additional fields beyond Entry
}

