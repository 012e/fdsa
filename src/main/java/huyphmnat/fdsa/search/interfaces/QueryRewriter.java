package huyphmnat.fdsa.search.interfaces;

/**
 * Service for rewriting user search queries to make them clearer and more effective.
 * Transforms natural language queries into more precise search terms.
 */
public interface QueryRewriter {

    /**
     * Rewrite a user's search query to make it clearer and more effective for search.
     * 
     * Example:
     * - Input: "good library for react UI"
     * - Output: "React component libraries"
     *
     * @param originalQuery the original user query
     * @return the rewritten, clearer query
     */
    String rewriteQuery(String originalQuery);
}
