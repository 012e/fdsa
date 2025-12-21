package huyphmnat.fdsa.repository.internal.services;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class JGitRepositoryInitializer implements GitInitializer {

    @Override
    public void initRepository(Path repoPath) {
        try {
            Files.createDirectories(repoPath);
            Git.init()
                    .setDirectory(repoPath.toFile())
                    .setBare(false)
                    .call()
                    .close();
        } catch (IOException | GitAPIException e) {
            log.error("Failed to initialize git repository at {}", repoPath, e);
            throw new RuntimeException("Failed to initialize git repository at " + repoPath, e);
        }
    }
}
