package huyphmnat.fdsa.base;

import huyphmnat.fdsa.repository.internal.services.RepositoryAuthorizationService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("integration-testing")
public class NoopsAuthorizationService implements RepositoryAuthorizationService {
    @Override
    public String extractOwnerFromIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new IllegalArgumentException("Repository identifier cannot be null or empty");
        }

        String[] parts = identifier.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid repository identifier format. Expected 'owner/repository'");
        }

        return parts[0];
    }

    @Override
    public void requireOwnership(UUID repositoryId) {

    }

    @Override
    public void requireOwnershipByIdentifier(String identifier) {

    }

    @Override
    public void validateOwnerMatchesCurrentUser(String identifier) {

    }
}
