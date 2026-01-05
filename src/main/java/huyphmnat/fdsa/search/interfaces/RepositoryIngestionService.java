package huyphmnat.fdsa.search.interfaces;

import java.util.UUID;

/**
 * Service for ingesting repositories into the search index.
 * This service handles the full-text indexing of repository files.
 */
public interface RepositoryIngestionService {

    /**
     * Ingest a repository into the search index.
     * This will recursively traverse the repository, extract code files,
     * and index them in OpenSearch for full-text search.
     *
     * @param repositoryId the UUID of the repository to ingest
     * @param repositoryIdentifier the unique identifier (owner/name) of the repository
     */
    void ingestRepository(UUID repositoryId, String repositoryIdentifier);
}

