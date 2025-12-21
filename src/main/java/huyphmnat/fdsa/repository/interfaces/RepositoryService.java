package huyphmnat.fdsa.repository.interfaces;

import huyphmnat.fdsa.repository.dtos.CreateRepositoryRequest;
import huyphmnat.fdsa.repository.dtos.Repository;

import java.util.List;

public interface RepositoryService {

    Repository createRepository(CreateRepositoryRequest request);

    Repository getRepository(String identifier);

    List<Repository> listRepositories();

    List<Repository> listRepositoriesByOwner(String owner);
}

