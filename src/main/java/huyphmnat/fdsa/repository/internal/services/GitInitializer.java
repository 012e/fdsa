package huyphmnat.fdsa.repository.internal.services;

import java.nio.file.Path;

public interface GitInitializer {
    void initRepository(Path repoPath);
}
