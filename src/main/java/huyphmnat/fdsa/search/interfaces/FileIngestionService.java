package huyphmnat.fdsa.search.interfaces;

import java.util.UUID;

/**
 * Service for ingesting individual files into the search index.
 * This service handles incremental updates to the search index when files change.
 */
public interface FileIngestionService {

    /**
     * Index a single file in the search index.
     * This will extract the file content, generate embeddings, and index it in OpenSearch.
     *
     * @param repositoryId the UUID of the repository
     * @param repositoryIdentifier the unique identifier (owner/name) of the repository
     * @param filePath the path to the file within the repository
     */
    void indexFile(UUID repositoryId, String repositoryIdentifier, String filePath);

    /**
     * Remove a single file from the search index.
     *
     * @param repositoryId the UUID of the repository
     * @param filePath the path to the file within the repository
     */
    void removeFile(UUID repositoryId, String filePath);

    /**
     * Remove all files within a folder from the search index.
     *
     * @param repositoryId the UUID of the repository
     * @param folderPath the path to the folder within the repository
     */
    void removeFolder(UUID repositoryId, String folderPath);
}
