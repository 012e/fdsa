package huyphmnat.fdsa.search.interfaces;

import huyphmnat.fdsa.search.dtos.SearchOptions;
import huyphmnat.fdsa.search.dtos.SearchResult;

public interface SearchService {
    SearchResult search(SearchOptions options);
}
