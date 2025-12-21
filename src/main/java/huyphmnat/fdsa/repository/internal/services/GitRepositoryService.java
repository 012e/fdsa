package huyphmnat.fdsa.repository.internal.services;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;

@Component
public class GitRepositoryService {

    public void stageAll(Path repoRoot) {
        try (Git git = Git.open(repoRoot.toFile())) {
            git.add().addFilepattern(".").call();
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to stage changes", e);
        }
    }

    public void commit(Path repoRoot, String message) {
        try (Git git = Git.open(repoRoot.toFile())) {
            if (git.status().call().isClean()) {
                return; // nothing to commit
            }
            git.commit()
                    .setMessage(message)
                    .setSign(false)
                    .call();
        } catch (IOException | GitAPIException e) {
            throw new RuntimeException("Failed to commit changes", e);
        }
    }
}

