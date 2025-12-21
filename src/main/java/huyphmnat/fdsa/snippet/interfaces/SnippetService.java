package huyphmnat.fdsa.snippet.interfaces;

import huyphmnat.fdsa.snippet.dtos.CreateSnippetRequest;
import huyphmnat.fdsa.snippet.dtos.Snippet;
import huyphmnat.fdsa.snippet.dtos.SnippetFile;
import huyphmnat.fdsa.snippet.dtos.UpdateSnippetRequest;

import java.util.List;
import java.util.UUID;

public interface SnippetService {
    Snippet createSnippet(CreateSnippetRequest request);
    Snippet getSnippet(UUID id);
    Snippet getSnippetByPath(String owner, String path);
    List<Snippet> getAllSnippets();
    List<Snippet> getSnippetsByOwner(String owner);
    Snippet updateSnippet(UpdateSnippetRequest request);
    void deleteSnippet(UUID id);
    List<SnippetFile> listFilesByPath(String owner, String path);
}
