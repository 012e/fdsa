package huyphmnat.fdsa.graph.controllers;

import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.stereotype.Controller;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Controller
@Slf4j
@RequiredArgsConstructor
public class RepositoryFileController {

    private final RepositoryFileService repositoryFileService;

    @MutationMapping
    public Boolean createRepositoryFolder(@Argument RepositoryPathChangeInput input) {
        UUID repositoryId = UUID.fromString(input.repositoryId());
        log.info("Creating folder in repository {} at path {} with message {}", repositoryId, input.path(), input.commitMessage());
        repositoryFileService.createFolder(repositoryId, input.path(), input.commitMessage());
        return true;
    }

    @MutationMapping
    public Boolean deleteRepositoryFolder(@Argument RepositoryPathChangeInput input) {
        UUID repositoryId = UUID.fromString(input.repositoryId());
        log.info("Deleting folder in repository {} at path {} with message {}", repositoryId, input.path(), input.commitMessage());
        repositoryFileService.deleteFolder(repositoryId, input.path(), input.commitMessage());
        return true;
    }

    @MutationMapping
    public Boolean addRepositoryFile(@Argument RepositoryFileChangeInput input) {
        UUID repositoryId = UUID.fromString(input.repositoryId());
        byte[] content = decodeContent(input.content(), input.encoding());
        log.info("Adding file in repository {} at path {} with message {}", repositoryId, input.path(), input.commitMessage());
        repositoryFileService.addFile(repositoryId, input.path(), content, input.commitMessage());
        return true;
    }

    @MutationMapping
    public Boolean updateRepositoryFile(@Argument RepositoryFileChangeInput input) {
        UUID repositoryId = UUID.fromString(input.repositoryId());
        byte[] content = decodeContent(input.content(), input.encoding());
        log.info("Updating file in repository {} at path {} with message {}", repositoryId, input.path(), input.commitMessage());
        repositoryFileService.updateFile(repositoryId, input.path(), content, input.commitMessage());
        return true;
    }

    @MutationMapping
    public Boolean deleteRepositoryFile(@Argument RepositoryPathChangeInput input) {
        UUID repositoryId = UUID.fromString(input.repositoryId());
        log.info("Deleting file in repository {} at path {} with message {}", repositoryId, input.path(), input.commitMessage());
        repositoryFileService.deleteFile(repositoryId, input.path(), input.commitMessage());
        return true;
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

    public record RepositoryPathChangeInput(String repositoryId, String path, String commitMessage) {}

    public record RepositoryFileChangeInput(String repositoryId, String path, String content, String commitMessage, RepositoryFileEncoding encoding) {}

    public enum RepositoryFileEncoding {
        TEXT,
        BASE64
    }
}

