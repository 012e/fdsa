package huyphmnat.fdsa.search.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Represents a code file document structure in OpenSearch.
 * This DTO maps directly to the document schema stored in the OpenSearch index.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeFileDocument {

    /**
     * Document ID
     */
    private String id;

    /**
     * Repository ID this file belongs to
     */
    @JsonProperty("repository_id")
    private String repositoryId;

    /**
     * Repository identifier (owner/name)
     */
    @JsonProperty("repository_identifier")
    private String repositoryIdentifier;

    /**
     * File path within the repository
     */
    @JsonProperty("file_path")
    private String filePath;

    /**
     * File name
     */
    @JsonProperty("file_name")
    private String fileName;

    /**
     * File extension
     */
    @JsonProperty("file_extension")
    private String fileExtension;

    /**
     * Programming language
     */
    private String language;

    /**
     * File content
     */
    private String content;

    /**
     * File size in bytes
     */
    private Long size;

    /**
     * Timestamp when document was created (ISO-8601 string)
     */
    @JsonProperty("created_at")
    private String createdAt;

    /**
     * Timestamp when document was last updated (ISO-8601 string)
     */
    @JsonProperty("updated_at")
    private String updatedAt;

    /**
     * Code chunks with metadata
     */
    private List<Chunk> chunks;

    /**
     * Represents a code chunk within the document
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chunk {
        /**
         * Chunk index
         */
        private Integer index;

        /**
         * Chunk content
         */
        private String content;

        /**
         * Starting line number
         */
        @JsonProperty("start_line")
        private Integer startLine;

        /**
         * Ending line number
         */
        @JsonProperty("end_line")
        private Integer endLine;
    }
}

