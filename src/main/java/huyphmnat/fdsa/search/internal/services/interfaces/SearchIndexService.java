package huyphmnat.fdsa.search.internal.services.interfaces;

import huyphmnat.fdsa.snippet.dtos.Snippet;

public interface SearchIndexService {
    void startIndexingSnippet(Snippet snippet);
}
