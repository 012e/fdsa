package huyphmnat.fdsa.repository;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.repository.dtos.CreateRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.Repository;
import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import huyphmnat.fdsa.repository.interfaces.RepositoryFileService;
import huyphmnat.fdsa.repository.interfaces.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

public class RepositoryFileServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RepositoryFileService repositoryFileService;

    @Autowired
    private RepositoryRepository repositoryRepository;

    @Test
    public void testAddUpdateDeleteFileAndFoldersWithGitCommits() throws Exception {
        // 1. Create a repository
        String identifier = "test-user/repo-file-ops-" + UUID.randomUUID();
        Repository repo = repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier(identifier)
                .description("Repository for file operations integration test")
                .build());

        assertThat(repo).isNotNull();
        Path repoPath = Paths.get(repo.getFilesystemPath());
        assertThat(Files.exists(repoPath)).isTrue();

        // Load the underlying entity to get the UUID id used by RepositoryFileService
        RepositoryEntity entity = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalStateException("Repository entity not found"));

        // Ensure git repo is initialized and clean initially
        try (Git git = Git.open(repoPath.toFile())) {
            assertThat(git.status().call().isClean()).as("Repository should be clean after initialization").isTrue();
        }

        // 2. Create a folder and verify .gitkeep and commit
        String folderPath = "src/main/java";
        String createFolderCommit = "Create src/main/java folder";
        repositoryFileService.createFolder(entity.getId(), folderPath, createFolderCommit);

        Path folder = repoPath.resolve(folderPath);
        assertThat(Files.exists(folder)).isTrue();
        assertThat(Files.isDirectory(folder)).isTrue();
        Path gitkeep = folder.resolve(".gitkeep");
        assertThat(Files.exists(gitkeep)).withFailMessage(".gitkeep should be created in new folder").isTrue();

        try (Git git = Git.open(repoPath.toFile())) {
            assertThat(git.status().call().isClean()).as("Repository should be clean after createFolder commit").isTrue();
            assertThat(git.log().setMaxCount(1).call().iterator().next().getFullMessage()).isEqualTo(createFolderCommit);
        }

        // 3. Add a new file and verify content and commit
        String filePath = "src/main/java/App.java";
        String fileContent = "public class App {}";
        String addFileCommit = "Add App.java";
        repositoryFileService.addFile(entity.getId(), filePath, fileContent.getBytes(StandardCharsets.UTF_8), addFileCommit);

        Path file = repoPath.resolve(filePath);
        assertThat(Files.exists(file)).isTrue();
        assertThat(Files.readString(file)).isEqualTo(fileContent);

        try (Git git = Git.open(repoPath.toFile())) {
            assertThat(git.status().call().isClean()).as("Repository should be clean after addFile commit").isTrue();
            assertThat(git.log().setMaxCount(1).call().iterator().next().getFullMessage()).isEqualTo(addFileCommit);
        }

        // 4. Update the file and verify new content and commit
        String updatedContent = "public class App { /* updated */ }";
        String updateFileCommit = "Update App.java";
        repositoryFileService.updateFile(entity.getId(), filePath, updatedContent.getBytes(StandardCharsets.UTF_8), updateFileCommit);

        assertThat(Files.readString(file)).isEqualTo(updatedContent);

        try (Git git = Git.open(repoPath.toFile())) {
            assertThat(git.status().call().isClean()).as("Repository should be clean after updateFile commit").isTrue();
            assertThat(git.log().setMaxCount(1).call().iterator().next().getFullMessage()).isEqualTo(updateFileCommit);
        }

        // 5. Delete the file and verify removal and commit
        String deleteFileCommit = "Delete App.java";
        repositoryFileService.deleteFile(entity.getId(), filePath, deleteFileCommit);

        assertThat(Files.exists(file)).isFalse();

        try (Git git = Git.open(repoPath.toFile())) {
            assertThat(git.status().call().isClean()).as("Repository should be clean after deleteFile commit").isTrue();
            assertThat(git.log().setMaxCount(1).call().iterator().next().getFullMessage()).isEqualTo(deleteFileCommit);
        }

        // 6. Delete folder recursively and verify commit
        String deleteFolderCommit = "Delete src/main/java folder";
        repositoryFileService.deleteFolder(entity.getId(), folderPath, deleteFolderCommit);

        assertThat(Files.exists(folder)).isFalse();

        try (Git git = Git.open(repoPath.toFile())) {
            assertThat(git.status().call().isClean()).as("Repository should be clean after deleteFolder commit").isTrue();
            assertThat(git.log().setMaxCount(1).call().iterator().next().getFullMessage()).isEqualTo(deleteFolderCommit);
        }
    }
}
