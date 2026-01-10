package huyphmnat.fdsa.search;

/**
 * Constants for OpenSearch index names used in the search module.
 * This class provides a centralized location for all index name constants
 * to avoid hard-coded strings throughout the codebase.
 */
public final class Indexes {

    /**
     * Index name for code files extracted from repositories
     */
    public static final String CODE_FILE_INDEX = "code_files";

    /**
     * Index name for standalone code snippets
     */
    public static final String CODE_SNIPPET_INDEX = "code_snippets";

    private Indexes() {
        // Prevent instantiation
    }
}
