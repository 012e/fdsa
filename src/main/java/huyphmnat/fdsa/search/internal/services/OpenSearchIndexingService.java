package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.dtos.CodeFileDocument;

import java.util.List;

public interface OpenSearchIndexingService {
    void indexCodeFile(CodeFileDocument document);

    void bulkIndexCodeFiles(List<CodeFileDocument> documents);
    void refreshIndexes();
}
