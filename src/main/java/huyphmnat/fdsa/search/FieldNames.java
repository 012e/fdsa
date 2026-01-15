package huyphmnat.fdsa.search;

/**
 * Constants for OpenSearch document field names used in the search module.
 * This class provides a centralized location for all field name constants
 * to avoid hard-coded strings throughout the codebase.
 */
public final class FieldNames {

    // Common fields
    public static final String ID = "id";
    public static final String CREATED_AT = "created_at";
    public static final String UPDATED_AT = "updated_at";

    // Repository-related fields
    public static final String REPOSITORY_ID = "repository_id";
    public static final String REPOSITORY_IDENTIFIER = "repository_identifier";
    public static final String REPOSITORY_IDENTIFIER_KEYWORD = "repository_identifier";

    // File-related fields
    public static final String FILE_PATH = "file_path";
    public static final String FILE_PATH_KEYWORD = "file_path.keyword";
    public static final String FILE_NAME = "file_name";
    public static final String FILE_NAME_KEYWORD = "file_name.keyword";
    public static final String FILE_EXTENSION = "file_extension";
    public static final String FILE_EXTENSION_KEYWORD = "file_extension.keyword";
    public static final String LANGUAGE = "language";
    public static final String LANGUAGE_KEYWORD = "language.keyword";
    public static final String CONTENT = "content";
    public static final String SIZE = "size";

    // Chunk-related fields
    public static final String CHUNKS = "chunks";
    public static final String CHUNK_INDEX = "index";
    public static final String CHUNK_CONTENT = "content";
    public static final String CHUNK_START_LINE = "start_line";
    public static final String CHUNK_END_LINE = "end_line";
    public static final String CHUNK_EMBEDDING = "embedding";

    // Embedding fields
    public static final String CONTENT_EMBEDDING = "content_embedding";

    // Summary fields
    public static final String CONTENT_SUMMARY = "content_summary";

    private FieldNames() {
        // Prevent instantiation
    }
}

