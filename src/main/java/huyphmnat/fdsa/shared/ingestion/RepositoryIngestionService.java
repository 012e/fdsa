package huyphmnat.fdsa.shared.ingestion;

import huyphmnat.fdsa.repository.dtos.DirectoryContent;
import huyphmnat.fdsa.repository.dtos.Entry;
import huyphmnat.fdsa.repository.dtos.FileContent;
import huyphmnat.fdsa.repository.dtos.FileEntryType;
import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class RepositoryIngestionService {

    private final RepositoryFileService repositoryFileService;
    private final SnippetIngestionService snippetIngestionService;

    @Async
    public void ingestRepository(UUID repositoryId, String identifier) {
        log.info("Starting repository ingestion for {} ({})", identifier, repositoryId);

        try {
            Queue<String> queue = new LinkedList<>();
            queue.add(""); // Start with root directory
            Set<String> visited = new HashSet<>();
            int filesProcessed = 0;

            while (!queue.isEmpty()) {
                String currentPath = queue.poll();
                if (visited.contains(currentPath)) {
                    continue;
                }
                visited.add(currentPath);

                log.info("Listing directory: {}", currentPath);
                DirectoryContent directoryContent = repositoryFileService.listDirectory(repositoryId, currentPath);

                for (Entry entry : directoryContent.getEntries()) {
                    String path = entry.getPath();

                    if (entry.getType() == FileEntryType.DIRECTORY) {
                        if (!visited.contains(path)) {
                            queue.add(path);
                        }
                    } else {
                        // It's a file - process it
                        processFile(repositoryId, identifier, path);
                        filesProcessed++;
                    }
                }
            }

            log.info("Completed repository ingestion for {}. Processed {} files", identifier, filesProcessed);
        } catch (Exception e) {
            log.error("Failed to ingest repository {}", identifier, e);
        }
    }

    @Async
    public void processFile(UUID repositoryId, String owner, String path) {
        try {
            log.info("Processing file: {} from repository {}", path, owner);

            // Fetch file content
            FileContent fileContent = repositoryFileService.readFile(repositoryId, path);
            String code = new String(fileContent.getContent(), StandardCharsets.UTF_8);

            // Generate snippet ID from owner and path
            String snippetId = owner + "/" + path;

            // Ingest the file as a snippet
            snippetIngestionService.ingestSnippet(snippetId, code);

            log.info("Successfully processed file: {}", path);
        } catch (Exception e) {
            log.error("Failed to process file {} from repository {}", path, owner, e);
        }
    }
}

