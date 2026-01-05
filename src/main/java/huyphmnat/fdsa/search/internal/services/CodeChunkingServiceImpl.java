package huyphmnat.fdsa.search.internal.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class CodeChunkingServiceImpl implements CodeChunkingService {

    private static final int CHUNK_SIZE = 512; // tokens
    private static final int CHARS_PER_TOKEN = 4; // rough estimate

    @Override
    public List<String> chunkCode(String code) {
        log.info("Chunking code...");

        // Simple chunking by approximate token count
        // For production, consider using a proper tokenizer
        int chunkSizeInChars = CHUNK_SIZE * CHARS_PER_TOKEN;
        List<String> chunks = new ArrayList<>();

        if (code.length() <= chunkSizeInChars) {
            chunks.add(code);
            return chunks;
        }

        // Split by lines to avoid breaking in the middle of lines
        String[] lines = code.split("\n");
        StringBuilder currentChunk = new StringBuilder();

        for (String line : lines) {
            if (currentChunk.length() + line.length() + 1 > chunkSizeInChars && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString());
                currentChunk = new StringBuilder();
            }
            currentChunk.append(line).append("\n");
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        log.info("Created {} chunks", chunks.size());
        return chunks;
    }
}

