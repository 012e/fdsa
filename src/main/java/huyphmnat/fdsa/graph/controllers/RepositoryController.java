package huyphmnat.fdsa.graph.controllers;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.Argument;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import huyphmnat.fdsa.repository.interfaces.RepositoryService;
import huyphmnat.fdsa.repository.dtos.Repository;
import huyphmnat.fdsa.repository.dtos.CreateRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.CloneRepositoryRequest;

@Controller
@Slf4j
@RequiredArgsConstructor
public class RepositoryController {

    private final RepositoryService repositoryService;

    @QueryMapping
    public Repository repository(@Argument String identifier) {
        log.info("Getting repository with identifier: {}", identifier);
        return repositoryService.getRepository(identifier);
    }

    @QueryMapping
    public List<Repository> repositories() {
        log.info("Listing all repositories");
        return repositoryService.listRepositories();
    }

    @QueryMapping
    public List<Repository> repositoriesByOwner(@Argument String owner) {
        log.info("Listing repositories for owner: {}", owner);
        return repositoryService.listRepositoriesByOwner(owner);
    }

    @MutationMapping
    public Repository createRepository(@Argument RepositoryInput input) {
        log.info("Creating repository with identifier: {}", input.identifier());
        CreateRepositoryRequest request = CreateRepositoryRequest.builder()
                .identifier(input.identifier())
                .description(input.description())
                .build();
        return repositoryService.createRepository(request);
    }

    @MutationMapping
    public Repository cloneRepository(@Argument CloneRepositoryInput input) {
        log.info("Cloning repository from {} with identifier: {}", input.sourceUrl(), input.identifier());
        CloneRepositoryRequest request = CloneRepositoryRequest.builder()
                .sourceUrl(input.sourceUrl())
                .identifier(input.identifier())
                .description(input.description())
                .build();
        return repositoryService.cloneRepository(request);
    }

    public record RepositoryInput(String identifier, String description) {}
    public record CloneRepositoryInput(String sourceUrl, String identifier, String description) {}
}