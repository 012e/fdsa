package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.interfaces.QueryRewriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class QueryRewriterImpl implements QueryRewriter {

    private final ChatClient.Builder chatClientBuilder;

    @Value("${search.query-rewriter.enabled:true}")
    private boolean queryRewriterEnabled;

    private static final String REWRITE_PROMPT = """
            You are a search query optimizer for a code search engine.
            
            Your task is to rewrite the user's search query to make it more precise and effective for searching code snippets.
            
            Guidelines:
            - Make the query more specific and technical
            - Use proper technical terminology
            - Remove conversational language and filler words
            - Keep it concise (ideally 2-5 words)
            - Focus on the core technical concept
            - If the query mentions a framework/library and a concept, combine them meaningfully
            
            Examples:
            - "good library for react UI" → "React component libraries"
            - "how to sort arrays" → "array sorting algorithms"
            - "best way to handle authentication in nodejs" → "Node.js authentication patterns"
            - "function to parse json" → "JSON parsing functions"
            - "error handling in python" → "Python exception handling"
            
            User query: {query}
            
            Return ONLY the rewritten query, nothing else. No explanations, no quotes, just the improved query.
            """;

    @Override
    public String rewriteQuery(String originalQuery) {
        if (!queryRewriterEnabled) {
            log.debug("Query rewriter is disabled, returning original query");
            return originalQuery;
        }

        if (originalQuery == null || originalQuery.trim().isEmpty()) {
            log.warn("Empty or null query provided, returning as-is");
            return originalQuery;
        }

        // For very short queries (1-2 words), often they're already clear enough
        String trimmedQuery = originalQuery.trim();
        if (trimmedQuery.split("\\s+").length <= 2) {
            log.debug("Query is already short ({} words), skipping rewrite", trimmedQuery.split("\\s+").length);
            return originalQuery;
        }

        try {
            log.debug("Rewriting query: {}", originalQuery);
            
            ChatClient chatClient = chatClientBuilder.build();
            PromptTemplate promptTemplate = new PromptTemplate(REWRITE_PROMPT);
            Prompt prompt = promptTemplate.create(Map.of("query", originalQuery));
            
            String rewrittenQuery = chatClient.prompt(prompt)
                    .call()
                    .content();
            
            String cleanedQuery = rewrittenQuery.trim()
                    .replaceAll("^[\"']|[\"']$", ""); // Remove surrounding quotes if present
            
            log.info("Query rewritten: '{}' → '{}'", originalQuery, cleanedQuery);
            return cleanedQuery;
            
        } catch (Exception e) {
            log.error("Error rewriting query, returning original: {}", e.getMessage(), e);
            return originalQuery;
        }
    }
}
