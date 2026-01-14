package huyphmnat.fdsa.repository.internal.services;

import huyphmnat.fdsa.repository.dtos.CloneRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.CreateRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.Repository;
import huyphmnat.fdsa.repository.dtos.RepositoryClonedEvent;
import huyphmnat.fdsa.repository.interfaces.RepositoryService;
import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import huyphmnat.fdsa.shared.events.EventService;
import io.micrometer.observation.annotation.Observed;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryServiceImpl implements RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final ModelMapper mapper;
    private final GitInitializer gitInitializer;
    private final EventService eventService;
    private final RepositoryAuthorizationService authorizationService;

    @Value("${repository.base-dir:./tmp/repos}")
    private String baseDir;

    @Override
    @Transactional
    @Observed
    public Repository createRepository(CreateRepositoryRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Repository identifier must be provided");
        }

        // Validate GitHub-style identifier format (username/repository)
        if (!identifier.matches("^[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Repository identifier must be in format 'username/repository' (e.g., 'jk/human-helper-source-code')");
        }

        // Validate that the current user is the owner in the identifier
        authorizationService.validateOwnerMatchesCurrentUser(identifier);

        if (repositoryRepository.existsByIdentifier(identifier)) {
            throw new IllegalStateException("Repository with identifier '" + identifier + "' already exists");
        }

        Path basePath = Paths.get(baseDir).toAbsolutePath();
        Path repoPath = basePath.resolve(identifier);

        gitInitializer.initRepository(repoPath);

        String ownerId = authorizationService.extractOwnerFromIdentifier(identifier);

        RepositoryEntity entity = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .identifier(identifier)
                .description(request.getDescription())
                .filesystemPath(repoPath.toString())
                .ownerId(ownerId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        repositoryRepository.save(entity);

        return mapper.map(entity, Repository.class);
    }

    private static String trimSlashes(String input) {
        if (input == null) return null;
        return input
                .replaceAll("^/+", "")
                .replaceAll("/+$", "");
    }

    @Override
    @Transactional
    @Observed
    public Repository cloneRepository(CloneRepositoryRequest request) {
        String sourceUrl = request.getSourceUrl();
        String identifier = request.getIdentifier();

        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new IllegalArgumentException("Source URL must be provided");
        }

        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Repository identifier must be provided");
        }

        // Validate GitHub-style identifier format (username/repository)
        if (!identifier.matches("^[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Repository identifier must be in format 'username/repository' (e.g., 'jk/human-helper-source-code')");
        }

        // Validate that the current user is the owner in the identifier
        authorizationService.validateOwnerMatchesCurrentUser(identifier);

        if (repositoryRepository.existsByIdentifier(identifier)) {
            throw new IllegalStateException("Repository with identifier '" + identifier + "' already exists");
        }

        Path basePath = Paths.get(baseDir).toAbsolutePath();
        Path repoPath = basePath.resolve(identifier);

        try {
            log.info("Cloning repository from {} to {}", sourceUrl, repoPath);
            Git.cloneRepository()
                    .setURI(sourceUrl)
                    .setDirectory(repoPath.toFile())
                    .call()
                    .close();
            log.info("Successfully cloned repository from {}", sourceUrl);
        } catch (GitAPIException e) {
            log.error("Failed to clone repository from {} to {}", sourceUrl, repoPath, e);
            throw new RuntimeException("Failed to clone repository from " + sourceUrl, e);
        }

        String ownerId = authorizationService.extractOwnerFromIdentifier(identifier);

        RepositoryEntity entity = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .identifier(trimSlashes(identifier))
                .description(request.getDescription())
                .filesystemPath(repoPath.toString())
                .ownerId(ownerId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        repositoryRepository.save(entity);

        // Publish repository.cloned event
        RepositoryClonedEvent event = RepositoryClonedEvent.builder()
                .id(entity.getId())
                .identifier(entity.getIdentifier())
                .sourceUrl(sourceUrl)
                .filesystemPath(entity.getFilesystemPath())
                .build();
        eventService.publish("repository.cloned", event);
        log.info("Published repository.cloned event for {}", identifier);

        return mapper.map(entity, Repository.class);
    }

    @Override
    @Transactional
    @Observed
    public Repository getRepository(String identifier) {
        identifier = trimSlashes(identifier);
        var entity = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
        return mapper.map(entity, Repository.class);
    }

    @Override
    @Transactional
    @Observed
    public List<Repository> listRepositories() {
        return repositoryRepository.findAll()
                .stream()
                .map(entity -> mapper.map(entity, Repository.class))
                .toList();
    }

    @Override
    @Transactional
    @Observed
    public List<Repository> listRepositoriesByOwner(String owner) {
        // Filter repositories by identifier prefix "owner/"
        return repositoryRepository.findByIdentifierStartingWith(owner + "/")
                .stream()
                .map(entity -> mapper.map(entity, Repository.class))
                .toList();
    }

    @Override
    @Transactional
    @Observed
    public void deleteRepository(String iden) {
        var identifier = trimSlashes(iden);
        
        // Validate that the current user is the owner
        authorizationService.validateOwnerMatchesCurrentUser(identifier);
        
        var entity = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("Repository not found: " + identifier));
        
        // Delete from database
        repositoryRepository.delete(entity);
        
        // Delete filesystem directory
        Path repoPath = Paths.get(entity.getFilesystemPath());
        try {
            if (java.nio.file.Files.exists(repoPath)) {
                deleteDirectoryRecursively(repoPath);
                log.info("Deleted repository directory: {}", repoPath);
            }
        } catch (Exception e) {
            log.error("Failed to delete repository directory: {}", repoPath, e);
            // Don't throw - database deletion already succeeded
        }
        
        log.info("Successfully deleted repository: {}", identifier);
    }
    
    private void deleteDirectoryRecursively(Path path) throws java.io.IOException {
        if (java.nio.file.Files.isDirectory(path)) {
            try (var stream = java.nio.file.Files.list(path)) {
                stream.forEach(child -> {
                    try {
                        deleteDirectoryRecursively(child);
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                });
            }
        }
        java.nio.file.Files.delete(path);
    }
}
