package huyphmnat.fdsa.search.interfaces;

import huyphmnat.fdsa.repository.dtos.RepositoryUpdatedEvent;

import java.util.List;
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

    /**
     * Ingest changed files from a repository update into the search index.
     * This will update, add, or remove files from the search index based on the change type.
     *
     * @param repositoryId the UUID of the repository
     * @param repositoryIdentifier the unique identifier (owner/name) of the repository
     * @param changedFiles the list of changed files with their content and change type
     */
    void ingestChangedFiles(UUID repositoryId, String repositoryIdentifier, List<RepositoryUpdatedEvent.ChangedFile> changedFiles);
}

