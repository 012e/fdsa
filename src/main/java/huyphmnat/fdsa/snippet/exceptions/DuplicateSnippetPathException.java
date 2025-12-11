package huyphmnat.fdsa.snippet.exceptions;

public class DuplicateSnippetPathException extends RuntimeException {

    public DuplicateSnippetPathException(String path) {
        super("A snippet with path '" + path + "' already exists");
    }
}

