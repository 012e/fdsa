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

public class RepositoryServiceTest extends BaseIntegrationTest {

    @Autowired
    private RepositoryService repositoryService;

    @Test
    public void testCreateRepository_CreatesGitRepoAndPersists() throws Exception {
        String identifier = "test-repo-1";

        Repository repo = repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier(identifier)
                .owner("testuser")
                .name("Test Repo")
                .description("A test repository")
                .build());

        assertNotNull(repo);
        assertEquals(identifier, repo.getIdentifier());
        assertEquals("testuser", repo.getOwner());
        assertEquals("Test Repo", repo.getName());
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
        String identifier = "duplicate-repo";

        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier(identifier)
                .owner("user1")
                .name("Repo 1")
                .build());

        assertThrows(IllegalStateException.class, () -> {
            repositoryService.createRepository(CreateRepositoryRequest.builder()
                    .identifier(identifier)
                    .owner("user2")
                    .name("Repo 2")
                    .build());
        });
    }

    @Test
    public void testListRepositoriesByOwner() {
        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier("owner1-repo1")
                .owner("owner1")
                .name("Repo 1")
                .build());

        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier("owner1-repo2")
                .owner("owner1")
                .name("Repo 2")
                .build());

        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier("owner2-repo1")
                .owner("owner2")
                .name("Repo 3")
                .build());

        var owner1Repos = repositoryService.listRepositoriesByOwner("owner1");
        assertEquals(2, owner1Repos.size());

        var owner2Repos = repositoryService.listRepositoriesByOwner("owner2");
        assertEquals(1, owner2Repos.size());
    }
}
