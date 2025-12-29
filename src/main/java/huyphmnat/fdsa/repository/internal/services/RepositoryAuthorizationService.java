package huyphmnat.fdsa.repository.internal.services;

import java.util.UUID;

public interface RepositoryAuthorizationService {
    String extractOwnerFromIdentifier(String identifier);

    void requireOwnership(UUID repositoryId);

    void requireOwnershipByIdentifier(String identifier);

    void validateOwnerMatchesCurrentUser(String identifier);
}
