package huyphmnat.fdsa.snippet;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.snippet.dtos.*;
import huyphmnat.fdsa.snippet.exceptions.DuplicateSnippetPathException;
import huyphmnat.fdsa.snippet.exceptions.SnippetNotFoundException;
import huyphmnat.fdsa.snippet.interfaces.SnippetService;
import huyphmnat.fdsa.snippet.internal.services.interfaces.EventService;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SnippetServiceTest extends BaseIntegrationTest {
    @Autowired
    private SnippetService snippetService;

    @MockitoBean
    private EventService eventService;

    @Captor
    private ArgumentCaptor<String> eventNameCaptor;

    @Captor
    private ArgumentCaptor<Object> eventPayloadCaptor;

    @Test
    public void testCreateSnippet() {
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .code("console.log('Hello, World!');")
                .path("/test/hello-world.js")
                .build());
        var createdSnippet = snippetService.getSnippet(newSnippet.getId());

        assertNotNull(createdSnippet);
        assertEquals(newSnippet.getCode(), createdSnippet.getCode());
        assertEquals(newSnippet.getId(), createdSnippet.getId());
        assertEquals(newSnippet.getPath(), createdSnippet.getPath());

        // Verify that the event was published correctly
        verify(eventService, times(1)).publish(eventNameCaptor.capture(), eventPayloadCaptor.capture());

        assertEquals("snippet.created", eventNameCaptor.getValue());

        Object payload = eventPayloadCaptor.getValue();
        assertInstanceOf(SnippetCreatedEvent.class, payload);

        SnippetCreatedEvent event = (SnippetCreatedEvent) payload;
        assertEquals(newSnippet.getId(), event.getId());
        assertEquals(newSnippet.getCode(), event.getCode());
    }

    @Test
    public void testGetSnippet() {
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .code("console.log('Test');")
                .path("/test/snippet.js")
                .build());

        var retrievedSnippet = snippetService.getSnippet(newSnippet.getId());

        assertNotNull(retrievedSnippet);
        assertEquals(newSnippet.getId(), retrievedSnippet.getId());
        assertEquals(newSnippet.getPath(), retrievedSnippet.getPath());
        assertEquals(newSnippet.getCode(), retrievedSnippet.getCode());
    }

    @Test
    public void testGetSnippet_NotFound() {
        var randomId = java.util.UUID.randomUUID();
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.getSnippet(randomId);
        });
    }

    @Test
    public void testGetAllSnippets() {
        // Create multiple snippets
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/first.js")
                .code("console.log('First');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/second.js")
                .code("console.log('Second');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/third.js")
                .code("console.log('Third');")
                .build());

        // Get all snippets
        var allSnippets = snippetService.getAllSnippets();

        assertNotNull(allSnippets);
        assertTrue(allSnippets.size() >= 3);

        // Verify that our created snippets are in the list
        var snippetIds = allSnippets.stream()
                .map(Snippet::getId)
                .toList();

        assertTrue(snippetIds.contains(snippet1.getId()));
        assertTrue(snippetIds.contains(snippet2.getId()));
        assertTrue(snippetIds.contains(snippet3.getId()));
    }

    @Test
    public void testUpdateSnippet() {
        // Create a snippet first
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/original.js")
                .code("console.log('Original');")
                .build());

        // Reset mock to clear previous invocations
        reset(eventService);

        // Update the snippet
        var updatedSnippet = snippetService.updateSnippet(UpdateSnippetRequest
                .builder()
                .id(newSnippet.getId())
                .path("/test/updated.js")
                .code("console.log('Updated');")
                .build());

        assertNotNull(updatedSnippet);
        assertEquals(newSnippet.getId(), updatedSnippet.getId());
        assertEquals("/test/updated.js", updatedSnippet.getPath());
        assertEquals("console.log('Updated');", updatedSnippet.getCode());

        // Verify the snippet was updated in database
        var retrievedSnippet = snippetService.getSnippet(newSnippet.getId());
        assertEquals("/test/updated.js", retrievedSnippet.getPath());
        assertEquals("console.log('Updated');", retrievedSnippet.getCode());

        // Verify that the update event was published correctly
        verify(eventService, times(1)).publish(eventNameCaptor.capture(), eventPayloadCaptor.capture());

        assertEquals("snippet.updated", eventNameCaptor.getValue());

        Object payload = eventPayloadCaptor.getValue();
        assertInstanceOf(SnippetUpdatedEvent.class, payload);

        SnippetUpdatedEvent event = (SnippetUpdatedEvent) payload;
        assertEquals(updatedSnippet.getId(), event.getId());
        assertEquals(updatedSnippet.getPath(), event.getPath());
        assertEquals(updatedSnippet.getCode(), event.getCode());
    }

    @Test
    public void testUpdateSnippet_NotFound() {
        var randomId = java.util.UUID.randomUUID();
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.updateSnippet(UpdateSnippetRequest
                    .builder()
                    .id(randomId)
                    .path("/test/fail.js")
                    .code("console.log('Will Fail');")
                    .build());
        });
    }

    @Test
    public void testDeleteSnippet() {
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/delete.js")
                .code("console.log('Hello, World!');")
                .build());

        // Reset mock to clear previous invocations
        reset(eventService);

        snippetService.deleteSnippet(newSnippet.getId());

        // Verify snippet is deleted
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.getSnippet(newSnippet.getId());
        });

        // Verify that the delete event was published correctly
        verify(eventService, times(1)).publish(eventNameCaptor.capture(), eventPayloadCaptor.capture());

        assertEquals("snippet.deleted", eventNameCaptor.getValue());

        Object payload = eventPayloadCaptor.getValue();
        assertInstanceOf(SnippetDeletedEvent.class, payload);

        SnippetDeletedEvent event = (SnippetDeletedEvent) payload;
        assertEquals(newSnippet.getId(), event.getId());
    }

    @Test
    public void testDeleteSnippet_NotFound() {
        var randomId = java.util.UUID.randomUUID();
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.deleteSnippet(randomId);
        });
    }

    @Test
    public void testGetSnippetByPath() {
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/by-path.js")
                .code("console.log('Find by path');")
                .build());

        var retrievedSnippet = snippetService.getSnippetByPath("/test/by-path.js");

        assertNotNull(retrievedSnippet);
        assertEquals(newSnippet.getId(), retrievedSnippet.getId());
        assertEquals(newSnippet.getPath(), retrievedSnippet.getPath());
        assertEquals(newSnippet.getCode(), retrievedSnippet.getCode());
    }

    @Test
    public void testGetSnippetByPath_NotFound() {
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.getSnippetByPath("/nonexistent/path.js");
        });
    }

    @Test
    public void testCreateSnippet_DuplicatePath() {
        snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/duplicate.js")
                .code("console.log('First');")
                .build());

        assertThrows(DuplicateSnippetPathException.class, () -> {
            snippetService.createSnippet(CreateSnippetRequest
                    .builder()
                    .path("/test/duplicate.js")
                    .code("console.log('Second');")
                    .build());
        });
    }

    @Test
    public void testUpdateSnippet_DuplicatePath() {
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/existing-path.js")
                .code("console.log('Existing');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/another-path.js")
                .code("console.log('Another');")
                .build());

        assertThrows(DuplicateSnippetPathException.class, () -> {
            snippetService.updateSnippet(UpdateSnippetRequest
                    .builder()
                    .id(snippet2.getId())
                    .path("/test/existing-path.js")
                    .code("console.log('Trying to use existing path');")
                    .build());
        });
    }

    @Test
    public void testUpdateSnippet_SamePath() {
        // Create a snippet
        var snippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/test/same-path.js")
                .code("console.log('Original');")
                .build());

        // Reset mock to clear previous invocations
        reset(eventService);

        // Update with the same path should work
        var updatedSnippet = snippetService.updateSnippet(UpdateSnippetRequest
                .builder()
                .id(snippet.getId())
                .path("/test/same-path.js")
                .code("console.log('Updated code');")
                .build());

        assertNotNull(updatedSnippet);
        assertEquals(snippet.getId(), updatedSnippet.getId());
        assertEquals("/test/same-path.js", updatedSnippet.getPath());
        assertEquals("console.log('Updated code');", updatedSnippet.getCode());
    }

    @Test
    public void testListFilesByPath_Directory() {
        // Create multiple snippets in the same directory
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/home/ok/file1.js")
                .code("console.log('File 1');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/home/ok/file2.js")
                .code("console.log('File 2');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/home/ok/file3.js")
                .code("console.log('File 3');")
                .build());

        // Create a snippet in a different directory
        snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/home/other/file.js")
                .code("console.log('Other');")
                .build());

        // List files in /home/ok/ directory (with trailing slash)
        var files = snippetService.listFilesByPath("/home/ok/");

        assertNotNull(files);
        assertEquals(3, files.size());

        var fileIds = files.stream()
                .map(SnippetFile::getId)
                .toList();

        assertTrue(fileIds.contains(snippet1.getId()));
        assertTrue(fileIds.contains(snippet2.getId()));
        assertTrue(fileIds.contains(snippet3.getId()));

        // Verify that code is not returned
        files.forEach(file -> {
            assertNotNull(file.getId());
            assertNotNull(file.getPath());
        });
    }

    @Test
    public void testListFilesByPath_SingleFile() {
        // Create a snippet
        var snippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/home/ok")
                .code("console.log('Single file');")
                .build());

        // Get specific file (without trailing slash)
        var files = snippetService.listFilesByPath("/home/ok");

        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals(snippet.getId(), files.get(0).getId());
        assertEquals("/home/ok", files.get(0).getPath());
        // Verify that code is not returned (SnippetFile doesn't have code field)
    }

    @Test
    public void testListFilesByPath_SingleFile_NotFound() {
        // Try to get a file that doesn't exist
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.listFilesByPath("/nonexistent/file.js");
        });
    }

    @Test
    public void testListFilesByPath_EmptyDirectory() {
        // List files in a directory that doesn't have any files
        var files = snippetService.listFilesByPath("/empty/directory/");

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void testListFilesByPath_NestedDirectories() {
        // Create snippets in nested directories
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/home/project/src/main.js")
                .code("console.log('Main');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/home/project/src/utils.js")
                .code("console.log('Utils');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/home/project/README.md")
                .code("# Readme")
                .build());

        // List all files in /home/project/src/
        var srcFiles = snippetService.listFilesByPath("/home/project/src/");
        assertEquals(2, srcFiles.size());

        // List all files in /home/project/ (should include all files starting with that prefix)
        var projectFiles = snippetService.listFilesByPath("/home/project/");
        assertEquals(3, projectFiles.size());
    }

    @Test
    public void testListFilesByPath_RootDirectory() {
        // Create snippets at different levels
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/root1.js")
                .code("console.log('Root 1');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/root2.js")
                .code("console.log('Root 2');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .path("/subdir/file.js")
                .code("console.log('Subdir');")
                .build());

        // List files at root level
        var rootFiles = snippetService.listFilesByPath("/");
        assertTrue(rootFiles.size() >= 3);

        var fileIds = rootFiles.stream()
                .map(SnippetFile::getId)
                .toList();

        assertTrue(fileIds.contains(snippet1.getId()));
        assertTrue(fileIds.contains(snippet2.getId()));
        assertTrue(fileIds.contains(snippet3.getId()));
    }
}
