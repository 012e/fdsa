package huyphmnat.fdsa.repository.internal.repositories;

import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RepositoryRepository extends JpaRepository<RepositoryEntity, UUID> {
    boolean existsByIdentifier(String identifier);

    Optional<RepositoryEntity> findByIdentifier(String identifier);

    List<RepositoryEntity> findByIdentifierStartingWith(String prefix);
}

