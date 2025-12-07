package huyphmnat.fdsa.snippet;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.snippet.dtos.CreateSnippetRequest;
import huyphmnat.fdsa.snippet.exceptions.SnippetNotFoundException;
import huyphmnat.fdsa.snippet.interfaces.SnippetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class SnippetServiceTest extends BaseIntegrationTest {
    @Autowired
    private SnippetService snippetService;

    @Test
    public void testCreateSnippet() {
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .code("console.log('Hello, World!');")
                .build());
        var createdSnippet = snippetService.getSnippet(newSnippet.getId());
        assertNotNull(createdSnippet);
        assertEquals(newSnippet.getCode(), createdSnippet.getCode());
        assertEquals(newSnippet.getId(), createdSnippet.getId());
    }

    @Test
    public void testDeleteSnippet() {
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .code("console.log('Hello, World!');")
                .build());
        snippetService.deleteSnippet(newSnippet.getId());
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.getSnippet(newSnippet.getId());
        });
    }
}
