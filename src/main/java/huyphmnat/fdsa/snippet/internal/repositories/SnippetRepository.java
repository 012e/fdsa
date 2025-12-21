package huyphmnat.fdsa.snippet.internal.repositories;

import huyphmnat.fdsa.snippet.internal.entites.SnippetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SnippetRepository extends JpaRepository<SnippetEntity, UUID> {
    boolean existsByOwnerAndPath(String owner, String path);
    java.util.Optional<SnippetEntity> findByOwnerAndPath(String owner, String path);
    java.util.List<SnippetEntity> findByOwnerAndPathStartingWith(String owner, String pathPrefix);
    java.util.List<SnippetEntity> findByOwner(String owner);
}
