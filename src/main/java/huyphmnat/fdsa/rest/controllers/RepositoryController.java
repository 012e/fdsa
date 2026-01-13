package huyphmnat.fdsa.rest.controllers;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import huyphmnat.fdsa.repository.interfaces.RepositoryService;
import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import huyphmnat.fdsa.repository.dtos.Repository;
import huyphmnat.fdsa.repository.dtos.CreateRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.CloneRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.DirectoryContent;
import huyphmnat.fdsa.repository.dtos.FileContent;

@RestController
@RequestMapping("/api/repositories")
@Slf4j
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;
    private final RepositoryFileService repositoryFileService;
    private final RepositoryRepository repositoryRepository;

    @GetMapping("/{owner}/{repository}")
    @Operation(operationId = "getRepository", summary = "Get a repository by owner and name",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, description = "Repository owner"),
            @Parameter(in = ParameterIn.PATH, name = "repository", required = true, description = "Repository name")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Repository details",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = Repository.class)))
        })
    public ResponseEntity<Repository> getRepository(
            @PathVariable String owner,
            @PathVariable String repository) {
        String identifier = owner + "/" + repository;
        log.info("Getting repository with identifier: {}", identifier);
        return ResponseEntity.ok(repositoryService.getRepository(identifier));
    }

    @GetMapping
    @Operation(operationId = "getAllRepositories", summary = "List all repositories",
        responses = {
            @ApiResponse(responseCode = "200", description = "List of repositories", 
                content = @Content(mediaType = "application/json"))
        })
    public ResponseEntity<List<Repository>> getAllRepositories() {
        log.info("Listing all repositories");
        return ResponseEntity.ok(repositoryService.listRepositories());
    }

    @GetMapping("/by-owner/{owner}")
    @Operation(operationId = "getRepositoriesByOwner", summary = "List repositories by owner",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, 
                description = "Repository owner", 
                allowReserved = true, 
                schema = @Schema(type = "string"))
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "List of repositories for owner", 
                content = @Content(mediaType = "application/json"))
        })
    public ResponseEntity<List<Repository>> getRepositoriesByOwner(@PathVariable String owner) {
        log.info("Listing repositories for owner: {}", owner);
        return ResponseEntity.ok(repositoryService.listRepositoriesByOwner(owner));
    }

    @PostMapping
    @Operation(operationId = "createRepository", summary = "Create a new repository",
        responses = {
            @ApiResponse(responseCode = "200", description = "Repository created", 
                content = @Content(mediaType = "application/json", 
                    schema = @Schema(implementation = Repository.class)))
        })
    public ResponseEntity<Repository> createRepository(@RequestBody RepositoryInput input) {
        log.info("Creating repository with identifier: {}", input.identifier());
        CreateRepositoryRequest request = CreateRepositoryRequest.builder()
                .identifier(input.identifier())
                .description(input.description())
                .build();
        return ResponseEntity.ok(repositoryService.createRepository(request));
    }

    @PostMapping("/clone")
    @Operation(operationId = "cloneRepository", summary = "Clone a repository from a source URL",
        responses = {
            @ApiResponse(responseCode = "200", description = "Repository cloned", 
                content = @Content(mediaType = "application/json", 
                    schema = @Schema(implementation = Repository.class)))
        })
    public ResponseEntity<Repository> cloneRepository(@RequestBody CloneRepositoryInput input) {
        log.info("Cloning repository from {} with identifier: {}", input.sourceUrl(), input.identifier());
        CloneRepositoryRequest request = CloneRepositoryRequest.builder()
                .sourceUrl(input.sourceUrl())
                .identifier(input.identifier())
                .description(input.description())
                .build();
        return ResponseEntity.ok(repositoryService.cloneRepository(request));
    }

    @GetMapping("/{owner}/{repository}/browse")
    @Operation(operationId = "listRepositoryDirectory", summary = "List contents of a directory in a repository",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, description = "Repository owner"),
            @Parameter(in = ParameterIn.PATH, name = "repository", required = true, description = "Repository name"),
            @Parameter(in = ParameterIn.QUERY, name = "path", required = false, description = "Directory path (empty or / for root)")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Directory contents",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = DirectoryContent.class)))
        })
    public ResponseEntity<DirectoryContent> listRepositoryDirectory(
            @PathVariable String owner,
            @PathVariable String repository,
            @RequestParam(required = false, defaultValue = "") String path) {
        String identifier = owner + "/" + repository;
        UUID repoId = resolveRepositoryId(identifier);
        log.info("Listing directory in repository {} at path: {}", identifier, path);
        return ResponseEntity.ok(repositoryFileService.listDirectory(repoId, path));
    }

    @GetMapping("/{owner}/{repository}/files")
    @Operation(operationId = "readRepositoryFile", summary = "Read contents of a file in a repository",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, description = "Repository owner"),
            @Parameter(in = ParameterIn.PATH, name = "repository", required = true, description = "Repository name"),
            @Parameter(in = ParameterIn.QUERY, name = "path", required = true, description = "File path")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "File contents",
                content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = FileContent.class)))
        })
    public ResponseEntity<FileContent> readRepositoryFile(
            @PathVariable String owner,
            @PathVariable String repository,
            @RequestParam String path) {
        String identifier = owner + "/" + repository;
        UUID repoId = resolveRepositoryId(identifier);
        log.info("Reading file in repository {} at path: {}", identifier, path);
        return ResponseEntity.ok(repositoryFileService.readFile(repoId, path));
    }

    // File and Folder Operations

    @PostMapping("/{owner}/{repository}/folders")
    @Operation(operationId = "createRepositoryFolder", summary = "Create a folder in a repository",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, description = "Repository owner"),
            @Parameter(in = ParameterIn.PATH, name = "repository", required = true, description = "Repository name")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Folder created")
        })
    public ResponseEntity<Boolean> createRepositoryFolder(
            @PathVariable String owner,
            @PathVariable String repository,
            @RequestBody RepositoryPathChangeInput input) {
        String identifier = owner + "/" + repository;
        UUID repoId = resolveRepositoryId(identifier);
        log.info("Creating folder in repository {} at path {} with message {}", identifier, input.path(), input.commitMessage());
        repositoryFileService.createFolder(repoId, input.path(), input.commitMessage());
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/{owner}/{repository}/folders")
    @Operation(operationId = "deleteRepositoryFolder", summary = "Delete a folder from a repository",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, description = "Repository owner"),
            @Parameter(in = ParameterIn.PATH, name = "repository", required = true, description = "Repository name")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Folder deleted")
        })
    public ResponseEntity<Boolean> deleteRepositoryFolder(
            @PathVariable String owner,
            @PathVariable String repository,
            @RequestBody RepositoryPathChangeInput input) {
        String identifier = owner + "/" + repository;
        UUID repoId = resolveRepositoryId(identifier);
        log.info("Deleting folder in repository {} at path {} with message {}", identifier, input.path(), input.commitMessage());
        repositoryFileService.deleteFolder(repoId, input.path(), input.commitMessage());
        return ResponseEntity.ok(true);
    }

    @PostMapping("/{owner}/{repository}/files")
    @Operation(operationId = "addRepositoryFile", summary = "Add a file to a repository",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, description = "Repository owner"),
            @Parameter(in = ParameterIn.PATH, name = "repository", required = true, description = "Repository name")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "File added")
        })
    public ResponseEntity<Boolean> addRepositoryFile(
            @PathVariable String owner,
            @PathVariable String repository,
            @RequestBody RepositoryFileChangeInput input) {
        String identifier = owner + "/" + repository;
        UUID repoId = resolveRepositoryId(identifier);
        byte[] content = decodeContent(input.content(), input.encoding());
        log.info("Adding file in repository {} at path {} with message {}", identifier, input.path(), input.commitMessage());
        repositoryFileService.addFile(repoId, input.path(), content, input.commitMessage());
        return ResponseEntity.ok(true);
    }

    @PutMapping("/{owner}/{repository}/files")
    @Operation(operationId = "updateRepositoryFile", summary = "Update a file in a repository",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, description = "Repository owner"),
            @Parameter(in = ParameterIn.PATH, name = "repository", required = true, description = "Repository name")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "File updated")
        })
    public ResponseEntity<Boolean> updateRepositoryFile(
            @PathVariable String owner,
            @PathVariable String repository,
            @RequestBody RepositoryFileChangeInput input) {
        String identifier = owner + "/" + repository;
        UUID repoId = resolveRepositoryId(identifier);
        byte[] content = decodeContent(input.content(), input.encoding());
        log.info("Updating file in repository {} at path {} with message {}", identifier, input.path(), input.commitMessage());
        repositoryFileService.updateFile(repoId, input.path(), content, input.commitMessage());
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/{owner}/{repository}/files")
    @Operation(operationId = "deleteRepositoryFile", summary = "Delete a file from a repository",
        parameters = {
            @Parameter(in = ParameterIn.PATH, name = "owner", required = true, description = "Repository owner"),
            @Parameter(in = ParameterIn.PATH, name = "repository", required = true, description = "Repository name")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "File deleted")
        })
    public ResponseEntity<Boolean> deleteRepositoryFile(
            @PathVariable String owner,
            @PathVariable String repository,
            @RequestBody RepositoryPathChangeInput input) {
        String identifier = owner + "/" + repository;
        UUID repoId = resolveRepositoryId(identifier);
        log.info("Deleting file in repository {} at path {} with message {}", identifier, input.path(), input.commitMessage());
        repositoryFileService.deleteFile(repoId, input.path(), input.commitMessage());
        return ResponseEntity.ok(true);
    }

    // Helper Methods

    private UUID resolveRepositoryId(String identifier) {
        return repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RuntimeException("Repository not found: " + identifier))
                .getId();
    }

    private byte[] decodeContent(String content, RepositoryFileEncoding encoding) {
        if (encoding == null || encoding == RepositoryFileEncoding.TEXT) {
            return content.getBytes(StandardCharsets.UTF_8);
        }
        if (encoding == RepositoryFileEncoding.BASE64) {
            return Base64.getDecoder().decode(content);
        }
        throw new IllegalArgumentException("Unsupported encoding: " + encoding);
    }

    // DTOs

    public record RepositoryInput(String identifier, String description) {}
    public record CloneRepositoryInput(String sourceUrl, String identifier, String description) {}
    public record RepositoryPathChangeInput(String path, String commitMessage) {}
    public record RepositoryFileChangeInput(String path, String content, String commitMessage, RepositoryFileEncoding encoding) {}

    public enum RepositoryFileEncoding {
        TEXT,
        BASE64
    }
}