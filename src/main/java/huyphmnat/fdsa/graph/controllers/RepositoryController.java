package huyphmnat.fdsa.graph.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import huyphmnat.fdsa.repository.interfaces.RepositoryService;
import huyphmnat.fdsa.repository.dtos.Repository;
import huyphmnat.fdsa.repository.dtos.CreateRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.CloneRepositoryRequest;

@RestController
@RequestMapping("/api/repositories")
@Slf4j
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    @GetMapping("/{identifier}")
    public ResponseEntity<Repository> getRepository(@PathVariable String identifier) {
        log.info("Getting repository with identifier: {}", identifier);
        return ResponseEntity.ok(repositoryService.getRepository(identifier));
    }

    @GetMapping
    public ResponseEntity<List<Repository>> getAllRepositories() {
        log.info("Listing all repositories");
        return ResponseEntity.ok(repositoryService.listRepositories());
    }

    @GetMapping("/by-owner/{owner}")
    public ResponseEntity<List<Repository>> getRepositoriesByOwner(@PathVariable String owner) {
        log.info("Listing repositories for owner: {}", owner);
        return ResponseEntity.ok(repositoryService.listRepositoriesByOwner(owner));
    }

    @PostMapping
    public ResponseEntity<Repository> createRepository(@RequestBody RepositoryInput input) {
        log.info("Creating repository with identifier: {}", input.identifier());
        CreateRepositoryRequest request = CreateRepositoryRequest.builder()
                .identifier(input.identifier())
                .description(input.description())
                .build();
        return ResponseEntity.ok(repositoryService.createRepository(request));
    }

    @PostMapping("/clone")
    public ResponseEntity<Repository> cloneRepository(@RequestBody CloneRepositoryInput input) {
        log.info("Cloning repository from {} with identifier: {}", input.sourceUrl(), input.identifier());
        CloneRepositoryRequest request = CloneRepositoryRequest.builder()
                .sourceUrl(input.sourceUrl())
                .identifier(input.identifier())
                .description(input.description())
                .build();
        return ResponseEntity.ok(repositoryService.cloneRepository(request));
    }

    public record RepositoryInput(String identifier, String description) {}
    public record CloneRepositoryInput(String sourceUrl, String identifier, String description) {}
}