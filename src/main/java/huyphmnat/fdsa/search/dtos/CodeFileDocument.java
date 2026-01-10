package huyphmnat.fdsa.search.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import huyphmnat.fdsa.search.FieldNames;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
    private UUID id;

    /**
     * Repository ID this file belongs to
     */
    @JsonProperty(FieldNames.REPOSITORY_ID)
    private UUID repositoryId;

    /**
     * Repository identifier (owner/name)
     */
    @JsonProperty(FieldNames.REPOSITORY_IDENTIFIER)
    private String repositoryIdentifier;

    /**
     * File path within the repository
     */
    @JsonProperty(FieldNames.FILE_PATH)
    private String filePath;

    /**
     * File name
     */
    @JsonProperty(FieldNames.FILE_NAME)
    private String fileName;

    /**
     * File extension
     */
    @JsonProperty(FieldNames.FILE_EXTENSION)
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
    @JsonProperty(FieldNames.CREATED_AT)
    private Instant createdAt;

    /**
     * Timestamp when document was last updated (ISO-8601 string)
     */
    @JsonProperty(FieldNames.UPDATED_AT)
    private Instant updatedAt;

    @JsonProperty(FieldNames.CHUNKS)
    /**
     * Code chunks with metadata
     */
    private List<CodeChunk> codeChunks;

    /**
     * Represents a code chunk within the document
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CodeChunk {
        /**
         * Chunk index
         */
        @JsonProperty(FieldNames.CHUNK_INDEX)
        private Integer index;

        /**
         * Chunk content
         */
        @JsonProperty(FieldNames.CHUNK_CONTENT)
        private String content;

        /**
         * Starting line number
         */
        @JsonProperty(FieldNames.CHUNK_START_LINE)
        private Integer startLine;

        /**
         * Ending line number
         */
        @JsonProperty(FieldNames.CHUNK_END_LINE)
        private Integer endLine;
    }
}

