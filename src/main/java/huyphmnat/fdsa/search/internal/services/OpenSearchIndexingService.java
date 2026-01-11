package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.dtos.CodeFileDocument;

import java.util.List;
import java.util.UUID;

public interface OpenSearchIndexingService {
    void indexCodeFile(CodeFileDocument document);

    void bulkIndexCodeFiles(List<CodeFileDocument> documents);

    void refreshIndexes();

    void deleteFilesByPaths(UUID repositoryId, List<String> filePaths);
}
