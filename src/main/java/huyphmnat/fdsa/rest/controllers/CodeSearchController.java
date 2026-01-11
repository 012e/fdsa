package huyphmnat.fdsa.rest.controllers;

import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;
import huyphmnat.fdsa.search.interfaces.CodeSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Code Search", description = "APIs for searching code files across repositories")
public class CodeSearchController {

    private final CodeSearchService codeSearchService;

    @GetMapping("/code")
    @Operation(
        operationId = "searchCode",
        summary = "Search for code files",
        description = "Performs full-text search across indexed code files with optional filters",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Search results",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CodeSearchResponse.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<CodeSearchResponse> searchCode(
            @RequestParam("q") String query,
            @RequestParam(required = false) String repositoryIdentifier,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String filePathPattern,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String highlight) {
        // Parse highlight fields
        List<String> highlightFields = null;
        if (highlight != null && !highlight.isEmpty()) {
            highlightFields = Arrays.asList(highlight.split(","));
        }

        CodeSearchRequest request = CodeSearchRequest.builder()
            .query(query)
            .repositoryIdentifier(repositoryIdentifier)
            .language(language)
            .filePathPattern(filePathPattern)
            .page(page)
            .size(size)
            .highlightFields(highlightFields)
            .build();

        CodeSearchResponse response = codeSearchService.searchCode(request);

        log.info("Search completed. Found {} results out of {} total hits",
            response.getResults().size(), response.getTotalHits());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/code")
    @Operation(
        operationId = "searchCodePost",
        summary = "Search for code files (POST)",
        description = "Performs full-text search across indexed code files with optional filters using POST request",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Search results",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CodeSearchResponse.class)
                )
            ),
            @ApiResponse(responseCode = "400", description = "Invalid search parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
        }
    )
    public ResponseEntity<CodeSearchResponse> searchCodePost(@RequestBody CodeSearchRequest request) {
        log.info("Searching code (POST) with query: '{}', page: {}, size: {}",
            request.getQuery(), request.getPage(), request.getSize());

        CodeSearchResponse response = codeSearchService.searchCode(request);

        log.info("Search completed. Found {} results out of {} total hits",
            response.getResults().size(), response.getTotalHits());

        return ResponseEntity.ok(response);
    }
}

