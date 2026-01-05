package huyphmnat.fdsa.repository.internal.services;

import huyphmnat.fdsa.repository.exceptions.RepositoryAccessDeniedException;
import huyphmnat.fdsa.repository.exceptions.RepositoryNotFoundException;
import huyphmnat.fdsa.repository.internal.entites.RepositoryEntity;
import huyphmnat.fdsa.repository.internal.repositories.RepositoryRepository;
import huyphmnat.fdsa.shared.security.JwtHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepositoryAuthorizationServiceTest {

    @Mock
    private RepositoryRepository repositoryRepository;

    @Mock
    private JwtHelper jwtHelper;

    @InjectMocks
    private RepositoryAuthorizationServiceImpl authorizationService;

    private static final String USER_ID = "95b54e9e-bbe3-4675-9c12-790ccf41dbb3";
    private static final String OTHER_USER_ID = "other-user-id";
    private static final UUID REPO_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        lenient().when(jwtHelper.getCurrentUserId()).thenReturn(USER_ID);
    }

    @Test
    void extractOwnerFromIdentifier_validIdentifier_returnsOwner() {
        String owner = authorizationService.extractOwnerFromIdentifier("jk/my-repo");
        assertThat(owner).isEqualTo("jk");
    }

    @Test
    void extractOwnerFromIdentifier_invalidIdentifier_throwsException() {
        assertThatThrownBy(() ->
            authorizationService.extractOwnerFromIdentifier("invalid-identifier")
        ).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void validateOwnerMatchesCurrentUser_matchingOwner_succeeds() {
        String identifier = USER_ID + "/my-repo";

        // Should not throw exception
        assertThatCode(() ->
            authorizationService.validateOwnerMatchesCurrentUser(identifier)
        ).doesNotThrowAnyException();
    }

    @Test
    void validateOwnerMatchesCurrentUser_differentOwner_throwsAccessDenied() {
        String identifier = OTHER_USER_ID + "/my-repo";

        assertThatThrownBy(() -> authorizationService.validateOwnerMatchesCurrentUser(identifier))
            .isInstanceOf(RepositoryAccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void requireOwnership_userIsOwner_succeeds() {
        RepositoryEntity repo = RepositoryEntity.builder()
            .id(REPO_ID)
            .identifier(USER_ID + "/test-repo")
            .ownerId(USER_ID)
            .build();

        when(repositoryRepository.findById(REPO_ID)).thenReturn(Optional.of(repo));

        // Should not throw exception
        assertThatCode(() ->
            authorizationService.requireOwnership(REPO_ID)
        ).doesNotThrowAnyException();
    }

    @Test
    void requireOwnership_userIsNotOwner_throwsAccessDenied() {
        RepositoryEntity repo = RepositoryEntity.builder()
            .id(REPO_ID)
            .identifier(OTHER_USER_ID + "/test-repo")
            .ownerId(OTHER_USER_ID)
            .build();

        when(repositoryRepository.findById(REPO_ID)).thenReturn(Optional.of(repo));

        assertThatThrownBy(() -> authorizationService.requireOwnership(REPO_ID))
            .isInstanceOf(RepositoryAccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }

    @Test
    void requireOwnership_repositoryNotFound_throwsNotFoundException() {
        when(repositoryRepository.findById(REPO_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.requireOwnership(REPO_ID))
            .isInstanceOf(RepositoryNotFoundException.class);
    }

    @Test
    void requireOwnershipByIdentifier_userIsOwner_succeeds() {
        String identifier = USER_ID + "/test-repo";
        RepositoryEntity repo = RepositoryEntity.builder()
            .id(REPO_ID)
            .identifier(identifier)
            .ownerId(USER_ID)
            .build();

        when(repositoryRepository.findByIdentifier(identifier)).thenReturn(Optional.of(repo));

        // Should not throw exception
        assertThatCode(() ->
            authorizationService.requireOwnershipByIdentifier(identifier)
        ).doesNotThrowAnyException();
    }

    @Test
    void requireOwnershipByIdentifier_userIsNotOwner_throwsAccessDenied() {
        String identifier = OTHER_USER_ID + "/test-repo";
        RepositoryEntity repo = RepositoryEntity.builder()
            .id(REPO_ID)
            .identifier(identifier)
            .ownerId(OTHER_USER_ID)
            .build();

        when(repositoryRepository.findByIdentifier(identifier)).thenReturn(Optional.of(repo));

        assertThatThrownBy(() -> authorizationService.requireOwnershipByIdentifier(identifier))
            .isInstanceOf(RepositoryAccessDeniedException.class)
            .hasMessageContaining("Access denied");
    }
}
