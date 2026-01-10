package huyphmnat.fdsa.search.interfaces;

import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;

/**
 * Service for searching code files in OpenSearch.
 * This service provides full-text search capabilities across indexed repository files.
 */
public interface CodeSearchService {

    /**
     * Search for code files in OpenSearch based on the provided search criteria.
     * Supports full-text search, filtering by repository, language, file type, and pagination.
     *
     * @param request the search request with query and filters
     * @return search response containing results and pagination information
     */
    CodeSearchResponse searchCode(CodeSearchRequest request);
}

