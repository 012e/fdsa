package huyphmnat.fdsa.repository.internal.services;

import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RepositoryFileServiceImpl implements RepositoryFileService {

    private final RepositoryPathResolver repositoryPathResolver;
    private final GitRepositoryService gitRepositoryService;

    @Override
    @Transactional
    public void addFile(UUID repositoryId, String path, byte[] content, String commitMessage) {
        Path repoRoot = repositoryPathResolver.getRepositoryRoot(repositoryId);
        Path targetPath = resolvePath(repoRoot, path);

        if (Files.exists(targetPath)) {
            throw new RuntimeException("File already exists: " + path);
        }

        try {
            if (targetPath.getParent() != null) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.write(targetPath, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to add file: " + path, e);
        }

        gitRepositoryService.stageAll(repoRoot);
        gitRepositoryService.commit(repoRoot, commitMessage);
    }

    @Override
    @Transactional
    public void updateFile(UUID repositoryId, String path, byte[] content, String commitMessage) {
        Path repoRoot = repositoryPathResolver.getRepositoryRoot(repositoryId);
        Path targetPath = resolvePath(repoRoot, path);

        if (!Files.exists(targetPath)) {
            throw new RuntimeException("File not found: " + path);
        }

        try {
            Files.write(targetPath, content, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new RuntimeException("Failed to update file: " + path, e);
        }

        gitRepositoryService.stageAll(repoRoot);
        gitRepositoryService.commit(repoRoot, commitMessage);
    }

    @Override
    @Transactional
    public void deleteFile(UUID repositoryId, String path, String commitMessage) {
        Path repoRoot = repositoryPathResolver.getRepositoryRoot(repositoryId);
        Path targetPath = resolvePath(repoRoot, path);

        if (!Files.exists(targetPath)) {
            throw new RuntimeException("File not found: " + path);
        }

        try {
            Files.delete(targetPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file: " + path, e);
        }

        gitRepositoryService.stageAll(repoRoot);
        gitRepositoryService.commit(repoRoot, commitMessage);
    }

    @Override
    @Transactional
    public void createFolder(UUID repositoryId, String path, String commitMessage) {
        Path repoRoot = repositoryPathResolver.getRepositoryRoot(repositoryId);
        Path folderPath = resolvePath(repoRoot, path);

        try {
            Files.createDirectories(folderPath);
            Path gitkeep = folderPath.resolve(".gitkeep");
            if (!Files.exists(gitkeep)) {
                Files.write(gitkeep, new byte[0], StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to create folder: " + path, e);
        }

        gitRepositoryService.stageAll(repoRoot);
        gitRepositoryService.commit(repoRoot, commitMessage);
    }

    @Override
    @Transactional
    public void deleteFolder(UUID repositoryId, String path, String commitMessage) {
        Path repoRoot = repositoryPathResolver.getRepositoryRoot(repositoryId);
        Path folderPath = resolvePath(repoRoot, path);

        if (!Files.exists(folderPath)) {
            throw new RuntimeException("Folder not found: " + path);
        }

        if (!Files.isDirectory(folderPath)) {
            throw new RuntimeException("Path is not a folder: " + path);
        }

        try {
            Files.walk(folderPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.delete(p);
                        } catch (IOException e) {
                            throw new RuntimeException("Failed to delete path: " + p, e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Failed to delete folder: " + path, e);
        }

        gitRepositoryService.stageAll(repoRoot);
        gitRepositoryService.commit(repoRoot, commitMessage);
    }

    private Path resolvePath(Path repoRoot, String relativePath) {
        Path normalized = repoRoot.resolve(relativePath).normalize();
        if (!normalized.startsWith(repoRoot)) {
            throw new RuntimeException("Invalid path (outside repository): " + relativePath);
        }
        return normalized;
    }
}

