package huyphmnat.fdsa.repository.internal.services;

import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RepositoryPathResolverImpl implements RepositoryPathResolver {

    private final RepositoryRepository repositoryRepository;

    @Override
    public Path getRepositoryRoot(UUID repositoryId) {
        RepositoryEntity entity = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RuntimeException("Repository not found"));
        return Paths.get(entity.getFilesystemPath()).toAbsolutePath().normalize();
    }
}

