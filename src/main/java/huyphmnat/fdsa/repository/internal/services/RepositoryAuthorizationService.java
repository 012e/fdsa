package huyphmnat.fdsa.repository.internal.services;

import huyphmnat.fdsa.repository.exceptions.RepositoryAccessDeniedException;
import huyphmnat.fdsa.repository.exceptions.RepositoryNotFoundException;
import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import huyphmnat.fdsa.shared.security.JwtHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Service to check repository ownership and authorization.
 * Similar to GitHub's permission model where only repository owners can modify their repositories.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RepositoryAuthorizationService {

    private final RepositoryRepository repositoryRepository;
    private final JwtHelper jwtHelper;

    /**
     * Extract owner from repository identifier (e.g., "jk/my-repo" -> "jk")
     */
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
     * Check if the current user is the owner of the repository
     */
    public boolean isOwner(String repositoryId) {
        String currentUserId = jwtHelper.getCurrentUserId();
        RepositoryEntity repository = repositoryRepository.findById(UUID.fromString(repositoryId))
                .orElseThrow(() -> new RepositoryNotFoundException("Repository not found: " + repositoryId));

        return currentUserId.equals(repository.getOwnerId());
    }

    /**
     * Check if the current user is the owner by repository identifier
     */
    public boolean isOwnerByIdentifier(String identifier) {
        String currentUserId = jwtHelper.getCurrentUserId();
        RepositoryEntity repository = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RepositoryNotFoundException("Repository not found: " + identifier));

        return currentUserId.equals(repository.getOwnerId());
    }

    /**
     * Require that the current user is the owner of the repository.
     * Throws RepositoryAccessDeniedException if not.
     */
    public void requireOwnership(UUID repositoryId) {
        String currentUserId = jwtHelper.getCurrentUserId();
        RepositoryEntity repository = repositoryRepository.findById(repositoryId)
                .orElseThrow(() -> new RepositoryNotFoundException("Repository not found: " + repositoryId));

        if (!currentUserId.equals(repository.getOwnerId())) {
            log.warn("Access denied: User {} attempted to access repository {} owned by {}",
                    currentUserId, repository.getIdentifier(), repository.getOwnerId());
            throw new RepositoryAccessDeniedException(
                    "Access denied: You don't have permission to modify this repository");
        }

        log.debug("Access granted: User {} is owner of repository {}", currentUserId, repository.getIdentifier());
    }

    /**
     * Require that the current user is the owner of the repository by identifier.
     * Throws RepositoryAccessDeniedException if not.
     */
    public void requireOwnershipByIdentifier(String identifier) {
        String currentUserId = jwtHelper.getCurrentUserId();
        RepositoryEntity repository = repositoryRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new RepositoryNotFoundException("Repository not found: " + identifier));

        if (!currentUserId.equals(repository.getOwnerId())) {
            log.warn("Access denied: User {} attempted to access repository {} owned by {}",
                    currentUserId, repository.getIdentifier(), repository.getOwnerId());
            throw new RepositoryAccessDeniedException(
                    "Access denied: You don't have permission to modify this repository");
        }

        log.debug("Access granted: User {} is owner of repository {}", currentUserId, repository.getIdentifier());
    }

    /**
     * Validate that the requesting user's ID matches the owner in the repository identifier.
     * For example, if user "jk" tries to create "other-user/repo", this will fail.
     */
    public void validateOwnerMatchesCurrentUser(String identifier) {
        String currentUserId = jwtHelper.getCurrentUserId();
        String ownerFromIdentifier = extractOwnerFromIdentifier(identifier);

        if (!currentUserId.equals(ownerFromIdentifier)) {
            log.warn("Access denied: User {} attempted to create/clone repository with identifier {} (owner mismatch)",
                    currentUserId, identifier);
            throw new RepositoryAccessDeniedException(
                    "Access denied: You can only create repositories under your own username. " +
                    "Expected identifier starting with '" + currentUserId + "/', but got '" + identifier + "'");
        }

        log.debug("Owner validation passed: User {} matches owner in identifier {}", currentUserId, identifier);
    }
}

