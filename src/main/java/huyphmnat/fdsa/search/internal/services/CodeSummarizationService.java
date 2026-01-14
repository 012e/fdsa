package huyphmnat.fdsa.search.internal.services;

/**
 * Service for generating summaries of code content using LLM.
 */
public interface CodeSummarizationService {
    
    /**
     * Generate a concise summary of the given code content.
     * The summary should capture the main purpose, functionality, and key components.
     *
     * @param code The source code to summarize
     * @param language The programming language of the code (optional, can be null)
     * @param filePath The file path (optional, provides context)
     * @return A concise summary of the code
     */
    String summarizeCode(String code, String language, String filePath);
}
