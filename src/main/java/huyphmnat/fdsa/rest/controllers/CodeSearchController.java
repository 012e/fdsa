package huyphmnat.fdsa.rest.controllers;

import huyphmnat.fdsa.search.dtos.CodeSearchRequest;
import huyphmnat.fdsa.search.dtos.CodeSearchResponse;
import huyphmnat.fdsa.search.interfaces.CodeSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
import java.util.UUID;

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
        parameters = {
            @Parameter(in = ParameterIn.QUERY, name = "q", required = true,
                description = "Search query text",
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.QUERY, name = "repositoryId",
                description = "Filter by repository ID",
                schema = @Schema(type = "string", format = "uuid")),
            @Parameter(in = ParameterIn.QUERY, name = "repositoryIdentifier",
                description = "Filter by repository identifier (owner/name)",
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.QUERY, name = "language",
                description = "Filter by programming language",
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.QUERY, name = "fileExtension",
                description = "Filter by file extension",
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.QUERY, name = "filePathPattern",
                description = "Filter by file path pattern (supports wildcards)",
                schema = @Schema(type = "string")),
            @Parameter(in = ParameterIn.QUERY, name = "page",
                description = "Page number (0-based)",
                schema = @Schema(type = "integer", defaultValue = "0")),
            @Parameter(in = ParameterIn.QUERY, name = "size",
                description = "Number of results per page",
                schema = @Schema(type = "integer", defaultValue = "10")),
            @Parameter(in = ParameterIn.QUERY, name = "highlight",
                description = "Fields to highlight (comma-separated)",
                schema = @Schema(type = "string"))
        },
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
            @RequestParam(required = false) UUID repositoryId,
            @RequestParam(required = false) String repositoryIdentifier,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String fileExtension,
            @RequestParam(required = false) String filePathPattern,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String highlight) {

        log.info("Searching code with query: '{}', page: {}, size: {}", query, page, size);

        // Parse highlight fields
        List<String> highlightFields = null;
        if (highlight != null && !highlight.isEmpty()) {
            highlightFields = Arrays.asList(highlight.split(","));
        }

        CodeSearchRequest request = CodeSearchRequest.builder()
            .query(query)
            .repositoryId(repositoryId)
            .repositoryIdentifier(repositoryIdentifier)
            .language(language)
            .fileExtension(fileExtension)
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

