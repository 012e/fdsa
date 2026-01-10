package huyphmnat.fdsa.repository.internal.services;

import huyphmnat.fdsa.repository.exceptions.RepositoryAccessDeniedException;
import huyphmnat.fdsa.repository.exceptions.RepositoryNotFoundException;
import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import huyphmnat.fdsa.shared.security.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service to check repository ownership and authorization.
 * Similar to GitHub's permission model where only repository owners can modify their repositories.
 */
@Service
@Profile("default")
@RequiredArgsConstructor
@Slf4j
public class RepositoryAuthorizationServiceImpl implements RepositoryAuthorizationService {

    private final RepositoryRepository repositoryRepository;
    private final JwtHelper jwtHelper;

    /**
     * Extract owner from repository identifier (e.g., "jk/my-repo" -> "jk")
     */
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

    /**
     * Require that the current user is the owner of the repository.
     * Throws RepositoryAccessDeniedException if not.
     */
    @Override
    public void requireOwnership(UUID repositoryId) {
        String currentUsername = jwtHelper.getPreferredUsername();
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new IllegalStateException("No username found in JWT token");
        }

        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException("Repository not found: " + repositoryId));

        if (!currentUsername.equals(repository.getOwnerId())) {
            log.warn("Access denied: User {} attempted to access repository {} owned by {}",
                    currentUsername, repository.getIdentifier(), repository.getOwnerId());
            throw new RepositoryAccessDeniedException(
                    "Access denied: You don't have permission to modify this repository");
        }

        log.debug("Access granted: User {} is owner of repository {}", currentUsername, repository.getIdentifier());
    }

    /**
     * Require that the current user is the owner of the repository by identifier.
     * Throws RepositoryAccessDeniedException if not.
     */
    @Override
    public void requireOwnershipByIdentifier(String identifier) {
        String currentUsername = jwtHelper.getPreferredUsername();
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new IllegalStateException("No username found in JWT token");
        }

        RepositoryEntity repository = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RepositoryNotFoundException("Repository not found: " + identifier));

        if (!currentUsername.equals(repository.getOwnerId())) {
            log.warn("Access denied: User {} attempted to access repository {} owned by {}",
                    currentUsername, repository.getIdentifier(), repository.getOwnerId());
            throw new RepositoryAccessDeniedException(
                    "Access denied: You don't have permission to modify this repository");
        }

        log.debug("Access granted: User {} is owner of repository {}", currentUsername, repository.getIdentifier());
    }

    /**
     * Validate that the requesting user's username matches the owner in the repository identifier.
     * For example, if user "jk" tries to create "other-user/repo", this will fail.
     */
    @Override
    public void validateOwnerMatchesCurrentUser(String identifier) {
        String currentUsername = jwtHelper.getPreferredUsername();
        if (currentUsername == null || currentUsername.isBlank()) {
            throw new IllegalStateException("No username found in JWT token");
        }

        String ownerFromIdentifier = extractOwnerFromIdentifier(identifier);

        if (!currentUsername.equals(ownerFromIdentifier)) {
            log.warn("Access denied: User {} attempted to create/clone repository with identifier {} (owner mismatch)",
                    currentUsername, identifier);
            throw new RepositoryAccessDeniedException(
                    "Access denied: You can only create repositories under your own username. " +
                    "Expected identifier starting with '" + currentUsername + "/', but got '" + identifier + "'");
        }

        log.debug("Owner validation passed: User {} matches owner in identifier {}", currentUsername, identifier);
    }
}

