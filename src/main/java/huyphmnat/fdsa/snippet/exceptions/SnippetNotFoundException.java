package huyphmnat.fdsa.snippet.exceptions;

public class SnippetNotFoundException extends RuntimeException {
    public SnippetNotFoundException(String message) {
        super(message);
    }

    public SnippetNotFoundException() {
        super("snippet not found");
    }
}
