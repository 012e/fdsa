package huyphmnat.fdsa.graph.controllers;

import huyphmnat.fdsa.snippet.dtos.CreateSnippetRequest;
import huyphmnat.fdsa.snippet.dtos.Snippet;
import huyphmnat.fdsa.snippet.dtos.UpdateSnippetRequest;
import huyphmnat.fdsa.snippet.interfaces.SnippetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
public class SnippetController {
    private final SnippetService snippetService;

    @QueryMapping
    public Snippet getSnippet(@Argument String id) {
        log.info("Getting snippet with id: {}", id);
        return snippetService.getSnippet(UUID.fromString(id));
    }

    @QueryMapping
    public Snippet getSnippetByPath(@Argument String path) {
        log.info("Getting snippet with path: {}", path);
        return snippetService.getSnippetByPath(path);
    }

    @QueryMapping
    public List<Snippet> getAllSnippets() {
        log.info("Getting all snippets");
        return snippetService.getAllSnippets();
    }

    @MutationMapping
    public Snippet createSnippet(@Argument SnippetInput snippet) {
        log.info("Creating snippet with path: {} and code: {}", snippet.path(), snippet.code());
        return snippetService.createSnippet(
                CreateSnippetRequest.builder()
                        .path(snippet.path())
                        .code(snippet.code())
                        .build()
        );
    }

    @MutationMapping
    public Snippet updateSnippet(@Argument String id, @Argument SnippetInput snippet) {
        log.info("Updating snippet with id: {}, path: {} and code: {}", id, snippet.path(), snippet.code());
        return snippetService.updateSnippet(
                UpdateSnippetRequest.builder()
                        .id(UUID.fromString(id))
                        .path(snippet.path())
                        .code(snippet.code())
                        .build()
        );
    }

    @MutationMapping
    public Boolean deleteSnippet(@Argument String id) {
        log.info("Deleting snippet with id: {}", id);
        snippetService.deleteSnippet(UUID.fromString(id));
        return true;
    }

    public record SnippetInput(String path, String code) {}
}
