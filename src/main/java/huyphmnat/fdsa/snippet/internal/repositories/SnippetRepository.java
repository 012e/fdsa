package huyphmnat.fdsa.snippet.internal.repositories;

import huyphmnat.fdsa.snippet.internal.entites.SnippetEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SnippetRepository extends JpaRepository<SnippetEntity, UUID> {
    boolean existsByPath(String path);
    java.util.Optional<SnippetEntity> findByPath(String path);
    java.util.List<SnippetEntity> findByPathStartingWith(String pathPrefix);
}
