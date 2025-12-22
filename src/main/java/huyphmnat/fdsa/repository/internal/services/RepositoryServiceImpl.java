package huyphmnat.fdsa.repository.internal.services;

import huyphmnat.fdsa.repository.dtos.CreateRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.Repository;
import huyphmnat.fdsa.repository.interfaces.RepositoryService;
import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
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
public class RepositoryServiceImpl implements RepositoryService {

    private final RepositoryRepository repositoryRepository;
    private final ModelMapper mapper;
    private final GitInitializer gitInitializer;

    @Value("${repository.base-dir:./tmp/repos}")
    private String baseDir;

    @Override
    @Transactional
    public Repository createRepository(CreateRepositoryRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Repository identifier must be provided");
        }

        // Validate GitHub-style identifier format (username/repository)
        if (!identifier.matches("^[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+$")) {
            throw new IllegalArgumentException("Repository identifier must be in format 'username/repository' (e.g., 'jk/human-helper-source-code')");
        }

        if (repositoryRepository.existsByIdentifier(identifier)) {
            throw new IllegalStateException("Repository with identifier '" + identifier + "' already exists");
        }

        Path basePath = Paths.get(baseDir).toAbsolutePath();
        Path repoPath = basePath.resolve(identifier);

        gitInitializer.initRepository(repoPath);

        RepositoryEntity entity = RepositoryEntity.builder()
                .id(UUID.randomUUID())
                .identifier(identifier)
                .description(request.getDescription())
                .filesystemPath(repoPath.toString())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        repositoryRepository.save(entity);

        return mapper.map(entity, Repository.class);
    }

    @Override
    @Transactional
    public Repository getRepository(String identifier) {
        var entity = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
        return mapper.map(entity, Repository.class);
    }

    @Override
    @Transactional
    public List<Repository> listRepositories() {
        return repositoryRepository.findAll()
                .stream()
                .map(entity -> mapper.map(entity, Repository.class))
                .toList();
    }

    @Override
    @Transactional
    public List<Repository> listRepositoriesByOwner(String owner) {
        // Filter repositories by identifier prefix "owner/"
        return repositoryRepository.findByIdentifierStartingWith(owner + "/")
                .stream()
                .map(entity -> mapper.map(entity, Repository.class))
                .toList();
    }
}
