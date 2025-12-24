package huyphmnat.fdsa.graph.controllers;

import huyphmnat.fdsa.snippet.dtos.CreateSnippetRequest;
import huyphmnat.fdsa.snippet.dtos.Snippet;
import huyphmnat.fdsa.snippet.dtos.SnippetFile;
import huyphmnat.fdsa.snippet.dtos.UpdateSnippetRequest;
import huyphmnat.fdsa.snippet.interfaces.SnippetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/snippets")
@Slf4j
@RequiredArgsConstructor
public class SnippetController {
    private final SnippetService snippetService;

    @GetMapping("/{id}")
    public ResponseEntity<Snippet> getSnippet(@PathVariable String id) {
        log.info("Getting snippet with id: {}", id);
        return ResponseEntity.ok(snippetService.getSnippet(UUID.fromString(id)));
    }

    @GetMapping("/by-path")
    public ResponseEntity<Snippet> getSnippetByPath(@RequestParam String owner, @RequestParam String path) {
        log.info("Getting snippet with owner: {} and path: {}", owner, path);
        return ResponseEntity.ok(snippetService.getSnippetByPath(owner, path));
    }

    @GetMapping
    public ResponseEntity<List<Snippet>> getAllSnippets() {
        log.info("Getting all snippets");
        return ResponseEntity.ok(snippetService.getAllSnippets());
    }

    @GetMapping("/by-owner/{owner}")
    public ResponseEntity<List<Snippet>> getSnippetsByOwner(@PathVariable String owner) {
        log.info("Getting snippets for owner: {}", owner);
        return ResponseEntity.ok(snippetService.getSnippetsByOwner(owner));
    }

    @GetMapping("/files")
    public ResponseEntity<List<SnippetFile>> listFilesByPath(@RequestParam String owner, @RequestParam String path) {
        log.info("Listing files by owner: {} and path: {}", owner, path);
        return ResponseEntity.ok(snippetService.listFilesByPath(owner, path));
    }

    @PostMapping
    public ResponseEntity<Snippet> createSnippet(@RequestBody SnippetInput snippet) {
        log.info("Creating snippet with owner: {}, path: {} and code: {}", snippet.owner(), snippet.path(), snippet.code());
        return ResponseEntity.ok(snippetService.createSnippet(
                CreateSnippetRequest.builder()
                        .owner(snippet.owner())
                        .path(snippet.path())
                        .code(snippet.code())
                        .build()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Snippet> updateSnippet(@PathVariable String id, @RequestBody SnippetInput snippet) {
        log.info("Updating snippet with id: {}, owner: {}, path: {} and code: {}", id, snippet.owner(), snippet.path(), snippet.code());
        return ResponseEntity.ok(snippetService.updateSnippet(
                UpdateSnippetRequest.builder()
                        .id(UUID.fromString(id))
                        .owner(snippet.owner())
                        .path(snippet.path())
                        .code(snippet.code())
                        .build()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Boolean> deleteSnippet(@PathVariable String id) {
        log.info("Deleting snippet with id: {}", id);
        snippetService.deleteSnippet(UUID.fromString(id));
        return ResponseEntity.ok(true);
    }

    public record SnippetInput(String owner, String path, String code) {}
}
