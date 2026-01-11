package huyphmnat.fdsa.repository.internal.services;

import huyphmnat.fdsa.repository.dtos.*;
import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import huyphmnat.fdsa.repository.topics.RepositoryTopics;
import huyphmnat.fdsa.shared.events.EventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryFileServiceImpl implements RepositoryFileService {

    private final RepositoryPathResolver repositoryPathResolver;
    private final GitRepositoryService gitRepositoryService;
    private final RepositoryAuthorizationService authorizationService;
    private final EventService eventService;
    private final huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository repositoryRepository;

    @Override
    @Transactional
    public void addFile(UUID repositoryId, String path, byte[] content, String commitMessage) {
        // Check ownership before allowing file addition
        authorizationService.requireOwnership(repositoryId);

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

        // Publish repository.updated event
        publishRepositoryUpdatedEvent(repositoryId, path, content, RepositoryUpdatedEvent.ChangeType.ADDED);
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

        // Publish repository.updated event
        publishRepositoryUpdatedEvent(repositoryId, path, content, RepositoryUpdatedEvent.ChangeType.MODIFIED);
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

        // Publish repository.updated event with empty content for deletion
        publishRepositoryUpdatedEvent(repositoryId, path, new byte[0], RepositoryUpdatedEvent.ChangeType.DELETED);
    }

    @Override
    @Transactional
    public void createFolder(UUID repositoryId, String path, String commitMessage) {
        // Check ownership before allowing folder creation
        authorizationService.requireOwnership(repositoryId);

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
        // Check ownership before allowing folder deletion
        authorizationService.requireOwnership(repositoryId);

        Path repoRoot = repositoryPathResolver.getRepositoryRoot(repositoryId);
        Path folderPath = resolvePath(repoRoot, path);

        if (!Files.exists(folderPath)) {
            throw new RuntimeException("Folder not found: " + path);
        }

        if (!Files.isDirectory(folderPath)) {
            throw new RuntimeException("Path is not a folder: " + path);
        }

        try (Stream<Path> pathStream = Files.walk(folderPath)) {
            pathStream.sorted(Comparator.reverseOrder())
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

    @Override
    public DirectoryContent listDirectory(UUID repositoryId, String path) {
        Path repoRoot = repositoryPathResolver.getRepositoryRoot(repositoryId);
        Path directoryPath = path == null || path.isEmpty() || path.equals("/")
            ? repoRoot
            : resolvePath(repoRoot, path);

        if (!Files.exists(directoryPath)) {
            throw new RuntimeException("Directory not found: " + path);
        }

        if (!Files.isDirectory(directoryPath)) {
            throw new RuntimeException("Path is not a directory: " + path);
        }

        try (Stream<Path> stream = Files.list(directoryPath)) {
            List<Entry> entries = stream
                .filter(p -> !p.getFileName().toString().startsWith(".git"))
                .map(p -> {
                    try {
                        String relativePath = repoRoot.relativize(p).toString();
                        String fileName = p.getFileName().toString();
                        boolean isDirectory = Files.isDirectory(p);

                        if (isDirectory) {
                            return DirectoryEntry.builder()
                                .path(relativePath)
                                .name(fileName)
                                .type(FileEntryType.DIRECTORY)
                                .build();
                        } else {
                            Long size = Files.size(p);
                            return FileEntry.builder()
                                .path(relativePath)
                                .name(fileName)
                                .type(FileEntryType.FILE)
                                .size(size)
                                .build();
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to read file info: " + p, e);
                    }
                })
                .sorted(Comparator
                    .comparing((Entry e) -> e.getType() == FileEntryType.FILE)
                    .thenComparing(Entry::getName))
                .collect(Collectors.toList());

            String displayPath = path == null || path.isEmpty() ? "/" : path;
            return DirectoryContent.builder()
                .path(displayPath)
                .entries(entries)
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to list directory: " + path, e);
        }
    }

    @Override
    public FileContent readFile(UUID repositoryId, String path) {
        Path repoRoot = repositoryPathResolver.getRepositoryRoot(repositoryId);
        Path filePath = resolvePath(repoRoot, path);

        if (!Files.exists(filePath)) {
            throw new RuntimeException("File not found: " + path);
        }

        if (!Files.isRegularFile(filePath)) {
            throw new RuntimeException("Path is not a file: " + path);
        }

        try {
            byte[] content = Files.readAllBytes(filePath);
            long size = Files.size(filePath);

            return FileContent.builder()
                .path(path)
                .name(filePath.getFileName().toString())
                .size(size)
                .content(content)
                .build();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + path, e);
        }
    }

    private void publishRepositoryUpdatedEvent(UUID repositoryId, String filePath, byte[] content, RepositoryUpdatedEvent.ChangeType changeType) {
        try {
            // Get repository identifier
            var repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RuntimeException("Repository not found: " + repositoryId));

            // Convert content to string (UTF-8)
            String contentString = new String(content, StandardCharsets.UTF_8);

            // Create changed file
            RepositoryUpdatedEvent.ChangedFile changedFile = RepositoryUpdatedEvent.ChangedFile.builder()
                .path(filePath)
                .content(contentString)
                .changeType(changeType)
                .build();

            // Create event
            RepositoryUpdatedEvent event = RepositoryUpdatedEvent.builder()
                .repositoryId(repositoryId)
                .identifier(repository.getIdentifier())
                .changedFiles(List.of(changedFile))
                .build();

            // Publish event
            eventService.publish(RepositoryTopics.REPOSITORY_UPDATED, event);
            log.info("Published repository.updated event for repository: {} ({}), file: {}, changeType: {}",
                repository.getIdentifier(), repositoryId, filePath, changeType);
        } catch (Exception e) {
            log.error("Failed to publish repository.updated event for repository: {}, file: {}",
                repositoryId, filePath, e);
            // Don't throw - event publishing failure shouldn't break file operations
        }
    }

    private Path resolvePath(Path repoRoot, String relativePath) {
        Path normalized = repoRoot.resolve(relativePath).normalize();
        if (!normalized.startsWith(repoRoot)) {
            throw new RuntimeException("Invalid path (outside repository): " + relativePath);
        }
        return normalized;
    }
}

