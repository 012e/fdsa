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

import static org.assertj.core.api.Assertions.*;

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

        assertThat(repo).isNotNull();
        assertThat(repo.getIdentifier()).isEqualTo(identifier);
        assertThat(repo.getFileSystemPath()).isNotNull();

        Path repoPath = Paths.get(repo.getFileSystemPath());
        assertThat(Files.exists(repoPath)).isTrue();
        assertThat(Files.isDirectory(repoPath)).isTrue();

        // Verify that a git repository has been initialized at this path
        File gitDir = repoPath.resolve(".git").toFile();
        assertThat(gitDir.exists()).isTrue();
        assertThat(gitDir.isDirectory()).isTrue();

        // Optionally, ensure JGit can open the repository
        try (Git opened = Git.open(repoPath.toFile())) {
            assertThat(opened.getRepository()).isNotNull();
        }
    }

    @Test
    public void testCreateRepository_DuplicateIdentifier() {
        String identifier = "test-user/duplicate-repo";

        repositoryService.createRepository(CreateRepositoryRequest.builder()
                .identifier(identifier)
                .build());

        assertThatThrownBy(() -> repositoryService.createRepository(CreateRepositoryRequest.builder()
                    .identifier(identifier)
                    .build()))
            .isInstanceOf(IllegalStateException.class);
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
        assertThat(testUserRepos.size()).isGreaterThanOrEqualTo(2);
    }
}
