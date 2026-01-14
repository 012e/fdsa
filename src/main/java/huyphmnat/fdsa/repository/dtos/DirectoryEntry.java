package huyphmnat.fdsa.repository.dtos;

import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class DirectoryEntry extends Entry {
    // Directories don't have additional fields beyond Entry
}

