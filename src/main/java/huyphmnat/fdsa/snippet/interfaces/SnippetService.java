package huyphmnat.fdsa.snippet.interfaces;

import huyphmnat.fdsa.snippet.dtos.CreateSnippetRequest;
import huyphmnat.fdsa.snippet.dtos.Snippet;
import huyphmnat.fdsa.snippet.dtos.UpdateSnippetRequest;

import java.util.UUID;

public interface SnippetService {
    Snippet createSnippet(CreateSnippetRequest request);
    Snippet getSnippet(UUID id);
    Snippet updateSnippet(UpdateSnippetRequest request);
    void deleteSnippet(UUID id);
}
