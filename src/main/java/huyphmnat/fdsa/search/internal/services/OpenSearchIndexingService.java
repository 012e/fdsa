package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.FieldNames;
import huyphmnat.fdsa.search.dtos.CodeFileDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface OpenSearchIndexingService {
    void indexCodeFile(CodeFileDocument document);

    void bulkIndexCodeFiles(List<CodeFileDocument> documents);
    void refreshIndex();
}
