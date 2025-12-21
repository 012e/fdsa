package huyphmnat.fdsa.repository.internal.services;

import java.nio.file.Path;
import java.util.UUID;

public interface RepositoryPathResolver {

    Path getRepositoryRoot(UUID repositoryId);
}

