package huyphmnat.fdsa.search.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import huyphmnat.fdsa.search.FieldNames;
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
    @JsonProperty(FieldNames.REPOSITORY_ID)
    private String repositoryId;

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
    private String createdAt;

    /**
     * Timestamp when document was last updated (ISO-8601 string)
     */
    @JsonProperty(FieldNames.UPDATED_AT)
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

