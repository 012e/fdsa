package huyphmnat.fdsa.search.internal.models;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a code file document in OpenSearch.
 * This document structure supports full-text search of repository files.
 */
@Data
@Builder
public class CodeFileDocument {

    /**
     * Unique identifier for this document (UUID)
     */
    private String id;

    /**
     * Repository ID this file belongs to
     */
    private UUID repositoryId;

    /**
     * Repository identifier (owner/name)
     */
    private String repositoryIdentifier;

    /**
     * File path within the repository (e.g., "src/main/java/Main.java")
     */
    private String filePath;

    /**
     * File name (e.g., "Main.java")
     */
    private String fileName;

    /**
     * File extension (e.g., "java", "py", "ts")
     */
    private String fileExtension;

    /**
     * Programming language detected (e.g., "Java", "Python", "TypeScript")
     */
    private String language;

    /**
     * Full content of the file
     */
    private String content;

    /**
     * File size in bytes
     */
    private Long size;

    /**
     * Code chunks for large files
     */
    private List<CodeChunk> chunks;

    /**
     * Timestamp when this document was created
     */
    private Instant createdAt;

    /**
     * Timestamp when this document was last updated
     */
    private Instant updatedAt;

    /**
     * Represents a chunk of code from a larger file
     */
    @Data
    @Builder
    public static class CodeChunk {
        /**
         * Index of this chunk (0-based)
         */
        private int index;

        /**
         * Content of this chunk
         */
        private String content;

        /**
         * Starting line number in the original file (1-based)
         */
        private int startLine;

        /**
         * Ending line number in the original file (1-based)
         */
        private int endLine;
    }
}

