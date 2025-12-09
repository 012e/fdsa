package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.dtos.SearchOptions;
import huyphmnat.fdsa.search.dtos.SearchResult;
import huyphmnat.fdsa.search.interfaces.SearchService;
import io.nexusrpc.Service;

@Service
public class SearchServiceImpl implements SearchService {
    @Override
    public SearchResult search(SearchOptions options) {
        return null;
    }
}
