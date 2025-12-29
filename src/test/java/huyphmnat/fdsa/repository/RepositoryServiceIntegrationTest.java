package huyphmnat.fdsa.repository;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.repository.dtos.CreateRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.Repository;
import huyphmnat.fdsa.repository.interfaces.RepositoryService;
import org.eclipse.jgit.api.Git;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class RepositoryServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private RepositoryService repositoryService;

    @Test
    public void testCreateRepository_CreatesGitRepoAndPersists() throws Exception {
        String identifier = "test-user/test-repo-1";

        Repository repo = repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier(identifier)
                .description("A test repository")
                .build());

        assertNotNull(repo);
        assertEquals(identifier, repo.getIdentifier());
        assertNotNull(repo.getFilesystemPath());

        Path repoPath = Paths.get(repo.getFilesystemPath());
        assertTrue(Files.exists(repoPath));
        assertTrue(Files.isDirectory(repoPath));

        // Verify that a git repository has been initialized at this path
        File gitDir = repoPath.resolve(".git").toFile();
        assertTrue(gitDir.exists(), ".git directory should exist");
        assertTrue(gitDir.isDirectory(), ".git should be a directory");

        // Optionally, ensure JGit can open the repository
        try (Git opened = Git.open(repoPath.toFile())) {
            assertNotNull(opened.getRepository());
        }
    }

    @Test
    public void testCreateRepository_DuplicateIdentifier() {
        String identifier = "test-user/duplicate-repo";

        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier(identifier)
                .build());

        assertThrows(IllegalStateException.class, () -> {
            repositoryService.createRepository(CreateRepositoryRequest.builder()
                    .identifier(identifier)
                    .build());
        });
    }

    @Test
    public void testListRepositoriesByOwner() {
        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier("test-user/repo1")
                .build());

        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier("test-user/repo2")
                .build());

        var testUserRepos = repositoryService.listRepositoriesByOwner("test-user");
        assertTrue(testUserRepos.size() >= 2, "Should have at least 2 repositories for test-user");
    }
}
