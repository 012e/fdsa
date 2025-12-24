package huyphmnat.fdsa.graph.controllers;

import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@RestController
@RequestMapping("/api/repositories/{repositoryId}")
@Slf4j
@RequiredArgsConstructor
public class RepositoryFileController {

    private final RepositoryFileService repositoryFileService;

    @PostMapping("/folders")
    public ResponseEntity<Boolean> createRepositoryFolder(
            @PathVariable String repositoryId,
            @RequestBody RepositoryPathChangeInput input) {
        UUID repoId = UUID.fromString(repositoryId);
        log.info("Creating folder in repository {} at path {} with message {}", repoId, input.path(), input.commitMessage());
        repositoryFileService.createFolder(repoId, input.path(), input.commitMessage());
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/folders")
    public ResponseEntity<Boolean> deleteRepositoryFolder(
            @PathVariable String repositoryId,
            @RequestBody RepositoryPathChangeInput input) {
        UUID repoId = UUID.fromString(repositoryId);
        log.info("Deleting folder in repository {} at path {} with message {}", repoId, input.path(), input.commitMessage());
        repositoryFileService.deleteFolder(repoId, input.path(), input.commitMessage());
        return ResponseEntity.ok(true);
    }

    @PostMapping("/files")
    public ResponseEntity<Boolean> addRepositoryFile(
            @PathVariable String repositoryId,
            @RequestBody RepositoryFileChangeInput input) {
        UUID repoId = UUID.fromString(repositoryId);
        byte[] content = decodeContent(input.content(), input.encoding());
        log.info("Adding file in repository {} at path {} with message {}", repoId, input.path(), input.commitMessage());
        repositoryFileService.addFile(repoId, input.path(), content, input.commitMessage());
        return ResponseEntity.ok(true);
    }

    @PutMapping("/files")
    public ResponseEntity<Boolean> updateRepositoryFile(
            @PathVariable String repositoryId,
            @RequestBody RepositoryFileChangeInput input) {
        UUID repoId = UUID.fromString(repositoryId);
        byte[] content = decodeContent(input.content(), input.encoding());
        log.info("Updating file in repository {} at path {} with message {}", repoId, input.path(), input.commitMessage());
        repositoryFileService.updateFile(repoId, input.path(), content, input.commitMessage());
        return ResponseEntity.ok(true);
    }

    @DeleteMapping("/files")
    public ResponseEntity<Boolean> deleteRepositoryFile(
            @PathVariable String repositoryId,
            @RequestBody RepositoryPathChangeInput input) {
        UUID repoId = UUID.fromString(repositoryId);
        log.info("Deleting file in repository {} at path {} with message {}", repoId, input.path(), input.commitMessage());
        repositoryFileService.deleteFile(repoId, input.path(), input.commitMessage());
        return ResponseEntity.ok(true);
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

    public record RepositoryPathChangeInput(String path, String commitMessage) {}

    public record RepositoryFileChangeInput(String path, String content, String commitMessage, RepositoryFileEncoding encoding) {}

    public enum RepositoryFileEncoding {
        TEXT,
        BASE64
    }
}
