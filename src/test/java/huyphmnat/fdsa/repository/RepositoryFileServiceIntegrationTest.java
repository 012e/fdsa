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

import static org.junit.jupiter.api.Assertions.*;

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
        String identifier = "repo-file-ops-" + UUID.randomUUID();
        Repository repo = repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier(identifier)
                .owner("file-ops-user")
                .name("File Ops Repo")
                .description("Repository for file operations integration test")
                .build());

        assertNotNull(repo);
        Path repoPath = Paths.get(repo.getFilesystemPath());
        assertTrue(Files.exists(repoPath));

        // Load the underlying entity to get the UUID id used by RepositoryFileService
        RepositoryEntity entity = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new IllegalStateException("Repository entity not found"));

        // Ensure git repo is initialized and clean initially
        try (Git git = Git.open(repoPath.toFile())) {
            assertTrue(git.status().call().isClean(), "Repository should be clean after initialization");
        }

        // 2. Create a folder and verify .gitkeep and commit
        String folderPath = "src/main/java";
        String createFolderCommit = "Create src/main/java folder";
        repositoryFileService.createFolder(entity.getId(), folderPath, createFolderCommit);

        Path folder = repoPath.resolve(folderPath);
        assertTrue(Files.exists(folder));
        assertTrue(Files.isDirectory(folder));
        Path gitkeep = folder.resolve(".gitkeep");
        assertTrue(Files.exists(gitkeep), ".gitkeep should be created in new folder");

        try (Git git = Git.open(repoPath.toFile())) {
            assertTrue(git.status().call().isClean(), "Repository should be clean after createFolder commit");
            assertEquals(createFolderCommit, git.log().setMaxCount(1).call().iterator().next().getFullMessage());
        }

        // 3. Add a new file and verify content and commit
        String filePath = "src/main/java/App.java";
        String fileContent = "public class App {}";
        String addFileCommit = "Add App.java";
        repositoryFileService.addFile(entity.getId(), filePath, fileContent.getBytes(StandardCharsets.UTF_8), addFileCommit);

        Path file = repoPath.resolve(filePath);
        assertTrue(Files.exists(file));
        assertEquals(fileContent, Files.readString(file));

        try (Git git = Git.open(repoPath.toFile())) {
            assertTrue(git.status().call().isClean(), "Repository should be clean after addFile commit");
            assertEquals(addFileCommit, git.log().setMaxCount(1).call().iterator().next().getFullMessage());
        }

        // 4. Update the file and verify new content and commit
        String updatedContent = "public class App { /* updated */ }";
        String updateFileCommit = "Update App.java";
        repositoryFileService.updateFile(entity.getId(), filePath, updatedContent.getBytes(StandardCharsets.UTF_8), updateFileCommit);

        assertEquals(updatedContent, Files.readString(file));

        try (Git git = Git.open(repoPath.toFile())) {
            assertTrue(git.status().call().isClean(), "Repository should be clean after updateFile commit");
            assertEquals(updateFileCommit, git.log().setMaxCount(1).call().iterator().next().getFullMessage());
        }

        // 5. Delete the file and verify removal and commit
        String deleteFileCommit = "Delete App.java";
        repositoryFileService.deleteFile(entity.getId(), filePath, deleteFileCommit);

        assertFalse(Files.exists(file));

        try (Git git = Git.open(repoPath.toFile())) {
            assertTrue(git.status().call().isClean(), "Repository should be clean after deleteFile commit");
            assertEquals(deleteFileCommit, git.log().setMaxCount(1).call().iterator().next().getFullMessage());
        }

        // 6. Delete folder recursively and verify commit
        String deleteFolderCommit = "Delete src/main/java folder";
        repositoryFileService.deleteFolder(entity.getId(), folderPath, deleteFolderCommit);

        assertFalse(Files.exists(folder));

        try (Git git = Git.open(repoPath.toFile())) {
            assertTrue(git.status().call().isClean(), "Repository should be clean after deleteFolder commit");
            assertEquals(deleteFolderCommit, git.log().setMaxCount(1).call().iterator().next().getFullMessage());
        }
    }
}
