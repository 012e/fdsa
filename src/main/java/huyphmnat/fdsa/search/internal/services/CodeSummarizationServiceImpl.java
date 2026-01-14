package huyphmnat.fdsa.search.internal.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CodeSummarizationServiceImpl implements CodeSummarizationService {

    private final ChatClient.Builder chatClientBuilder;

    private static final int MAX_CODE_LENGTH = 8000; // Limit code length to prevent token overflow
    
    private static final String SUMMARIZATION_PROMPT = """
            You are a code analysis assistant. Generate a concise, informative summary of the provided code.
            
            Guidelines:
            - Describe what the code does in 2-4 sentences
            - Mention the main classes, functions, or components
            - Highlight key functionality and purpose
            - Note any important patterns, algorithms, or techniques used
            - Keep it technical but clear
            - Do not include code snippets in the summary
            
            Language: {language}
            File path: {filePath}
            
            Code:
            ```
            {code}
            ```
            
            Return ONLY the summary, nothing else. No preamble, no explanation, just the summary.
            """;

    @Override
    public String summarizeCode(String code, String language, String filePath) {
        if (code == null || code.trim().isEmpty()) {
            log.warn("Empty code provided for summarization");
            return "Empty file";
        }

        try {
            // Truncate very long code to avoid token limits
            String codeToSummarize = code.length() > MAX_CODE_LENGTH 
                    ? code.substring(0, MAX_CODE_LENGTH) + "\n... (truncated)"
                    : code;

            log.debug("Generating summary for code of length: {} (language: {}, file: {})", 
                    code.length(), language, filePath);

            ChatClient chatClient = chatClientBuilder.build();
            PromptTemplate promptTemplate = new PromptTemplate(SUMMARIZATION_PROMPT);
            
            Map<String, Object> variables = Map.of(
                    "code", codeToSummarize,
                    "language", language != null ? language : "Unknown",
                    "filePath", filePath != null ? filePath : "Unknown"
            );
            
            Prompt prompt = promptTemplate.create(variables);
            
            String summary = chatClient.prompt(prompt)
                    .call()
                    .content();
            
            String cleanedSummary = summary.trim();
            
            log.debug("Generated summary of length: {}", cleanedSummary.length());
            return cleanedSummary;
            
        } catch (Exception e) {
            log.error("Failed to generate code summary for file: {}", filePath, e);
            // Fallback to a basic summary
            return String.format("Code file in %s (%d bytes)", 
                    language != null ? language : "unknown language", 
                    code.length());
        }
    }
}
