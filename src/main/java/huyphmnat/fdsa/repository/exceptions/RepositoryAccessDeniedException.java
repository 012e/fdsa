package huyphmnat.fdsa.repository.exceptions;

public class RepositoryAccessDeniedException extends RuntimeException {
    public RepositoryAccessDeniedException(String message) {
        super(message);
    }
}
