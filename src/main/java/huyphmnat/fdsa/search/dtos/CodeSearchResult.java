package huyphmnat.fdsa.search.dtos;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a code file search result from OpenSearch.
 */
@Data
@Builder
public class CodeSearchResult {

    /**
     * Document ID
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
     * File path within the repository
     */
    private String filePath;

    /**
     * File name
     */
    private String fileName;

    /**
     * File extension
     */
    private String fileExtension;

    /**
     * Programming language
     */
    private String language;

    /**
     * File content (may be truncated in search results)
     */
    private String content;

    /**
     * File size in bytes
     */
    private Long size;

    /**
     * Search relevance score
     */
    private Double score;

    /**
     * Highlighted snippets from the search
     */
    private Map<String, List<String>> highlights;

    /**
     * Timestamp when document was created
     */
    private Instant createdAt;

    /**
     * Timestamp when document was last updated
     */
    private Instant updatedAt;

    /**
     * Matched chunks (if content was chunked)
     */
    private List<ChunkMatch> matchedChunks;

    @Data
    @Builder
    public static class ChunkMatch {
        private int index;
        private String content;
        private int startLine;
        private int endLine;
        private List<String> highlights;
    }
}

