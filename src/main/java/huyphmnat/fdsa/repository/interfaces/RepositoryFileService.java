package huyphmnat.fdsa.repository.interfaces;

import java.util.UUID;

public interface RepositoryFileService {
    void deleteFolder(UUID repositoryId, String path, String commitMessage);

    void createFolder(UUID repositoryId, String path, String commitMessage);

    void deleteFile(UUID repositoryId, String path, String commitMessage);

    void updateFile(UUID repositoryId, String path, byte[] content, String commitMessage);

    void addFile(UUID repositoryId, String path, byte[] content, String commitMessage);
}



