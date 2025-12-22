package huyphmnat.fdsa.snippet;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.shared.events.EventService;
import huyphmnat.fdsa.snippet.dtos.*;
import huyphmnat.fdsa.snippet.exceptions.DuplicateSnippetPathException;
import huyphmnat.fdsa.snippet.exceptions.SnippetNotFoundException;
import huyphmnat.fdsa.snippet.interfaces.SnippetService;
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
                .owner("testuser")
                .path("/test/hello-world.js")
                .code("not important")
                .build());
        var createdSnippet = snippetService.getSnippet(newSnippet.getId());

        assertNotNull(createdSnippet);
        assertEquals(newSnippet.getCode(), createdSnippet.getCode());
        assertEquals(newSnippet.getOwner(), createdSnippet.getOwner());
        assertEquals(newSnippet.getPath(), createdSnippet.getPath());

        // Verify that the event was published correctly
        verify(eventService, times(1)).publish(eventNameCaptor.capture(), eventPayloadCaptor.capture());

        assertEquals("snippet.created", eventNameCaptor.getValue());

        Object payload = eventPayloadCaptor.getValue();
        assertInstanceOf(SnippetCreatedEvent.class, payload);

        SnippetCreatedEvent event = (SnippetCreatedEvent) payload;
        assertEquals(newSnippet.getId(), event.getId());
        assertEquals(newSnippet.getCode(), event.getCode());
        assertEquals(newSnippet.getOwner(), event.getOwner());
    }

    @Test
    public void testGetSnippet() {
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
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
                .owner("testuser")
                .path("/test/first.js")
                .code("console.log('First');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/test/second.js")
                .code("console.log('Second');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
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
                .owner("testuser")
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
                .owner("testuser")
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
                .owner("testuser")
                .path("/test/by-path.js")
                .code("console.log('Find by path');")
                .build());

        var retrievedSnippet = snippetService.getSnippetByPath("testuser", "/test/by-path.js");

        assertNotNull(retrievedSnippet);
        assertEquals(newSnippet.getId(), retrievedSnippet.getId());
        assertEquals(newSnippet.getPath(), retrievedSnippet.getPath());
        assertEquals(newSnippet.getCode(), retrievedSnippet.getCode());
    }

    @Test
    public void testGetSnippetByPath_NotFound() {
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.getSnippetByPath("testuser", "/nonexistent/path.js");
        });
    }

    @Test
    public void testCreateSnippet_DuplicatePath() {
        snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/test/duplicate.js")
                .code("console.log('First');")
                .build());

        assertThrows(DuplicateSnippetPathException.class, () -> {
            snippetService.createSnippet(CreateSnippetRequest
                    .builder()
                    .owner("testuser")
                    .path("/test/duplicate.js")
                    .code("console.log('Second');")
                    .build());
        });
    }

    @Test
    public void testCreateSnippet_SamePathDifferentOwners() {
        // Same path should work for different owners
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("user1")
                .path("/test/shared-name.js")
                .code("console.log('User 1');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("user2")
                .path("/test/shared-name.js")
                .code("console.log('User 2');")
                .build());

        assertNotNull(snippet1);
        assertNotNull(snippet2);
        assertNotEquals(snippet1.getId(), snippet2.getId());
        assertEquals("/test/shared-name.js", snippet1.getPath());
        assertEquals("/test/shared-name.js", snippet2.getPath());
        assertEquals("user1", snippet1.getOwner());
        assertEquals("user2", snippet2.getOwner());

        // Verify they can be retrieved separately by owner and path
        var retrieved1 = snippetService.getSnippetByPath("user1", "/test/shared-name.js");
        var retrieved2 = snippetService.getSnippetByPath("user2", "/test/shared-name.js");

        assertEquals(snippet1.getId(), retrieved1.getId());
        assertEquals(snippet2.getId(), retrieved2.getId());
    }

    @Test
    public void testUpdateSnippet_DuplicatePath() {
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/test/existing-path.js")
                .code("console.log('Existing');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
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
                .owner("testuser")
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
                .owner("testuser")
                .path("/home/ok/file1.js")
                .code("console.log('File 1');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/home/ok/file2.js")
                .code("console.log('File 2');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/home/ok/file3.js")
                .code("console.log('File 3');")
                .build());

        // Create a snippet in a different directory
        snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/home/other/file.js")
                .code("console.log('Other');")
                .build());

        // List files in /home/ok/ directory (with trailing slash)
        var files = snippetService.listFilesByPath("testuser", "/home/ok/");

        assertNotNull(files);
        assertEquals(3, files.size());

        var fileIds = files.stream()
                .map(SnippetFile::getId)
                .toList();

        assertTrue(fileIds.contains(snippet1.getId()));
        assertTrue(fileIds.contains(snippet2.getId()));
        assertTrue(fileIds.contains(snippet3.getId()));

        // Verify that code is not returned and all are files (not directories)
        files.forEach(file -> {
            assertNotNull(file.getId());
            assertNotNull(file.getPath());
            assertFalse(file.isDirectory());
        });
    }

    @Test
    public void testListFilesByPath_SingleFile() {
        // Create a snippet
        var snippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/home/single/file.js")
                .code("console.log('Single file');")
                .build());

        // Get specific file by listing directory
        var files = snippetService.listFilesByPath("testuser", "/home/single/");

        assertNotNull(files);
        assertEquals(1, files.size());
        assertEquals(snippet.getId(), files.get(0).getId());
        assertEquals("/home/single/file.js", files.get(0).getPath());
        assertFalse(files.get(0).isDirectory());
    }


    @Test
    public void testListFilesByPath_EmptyDirectory() {
        // List files in a directory that doesn't have any files
        var files = snippetService.listFilesByPath("testuser", "/empty/directory/");

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void testListFilesByPath_NestedDirectories() {
        // Create snippets in nested directories
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/home/project/src/main.js")
                .code("console.log('Main');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/home/project/src/utils.js")
                .code("console.log('Utils');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/home/project/README.md")
                .code("# Readme")
                .build());

        // List all files in /home/project/src/ (should show 2 files)
        var srcFiles = snippetService.listFilesByPath("testuser", "/home/project/src/");
        assertEquals(2, srcFiles.size());
        srcFiles.forEach(file -> {
            assertFalse(file.isDirectory());
            assertNotNull(file.getId());
        });

        // List all entries in /home/project/ (should show 1 file and 1 directory = 2 items)
        var projectFiles = snippetService.listFilesByPath("testuser", "/home/project/");
        assertEquals(2, projectFiles.size());

        // Verify we have one directory (src/) and one file (README.md)
        var directories = projectFiles.stream()
                .filter(SnippetFile::isDirectory)
                .toList();
        var files = projectFiles.stream()
                .filter(file -> !file.isDirectory())
                .toList();

        assertEquals(1, directories.size());
        assertEquals("/home/project/src/", directories.get(0).getPath());
        assertNull(directories.get(0).getId());

        assertEquals(1, files.size());
        assertEquals(snippet3.getId(), files.get(0).getId());
        assertEquals("/home/project/README.md", files.get(0).getPath());
    }

    @Test
    public void testListFilesByPath_RootDirectory() {
        // Create snippets at different levels
        var snippet1 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/root1.js")
                .code("console.log('Root 1');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/root2.js")
                .code("console.log('Root 2');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .owner("testuser")
                .path("/subdir/file.js")
                .code("console.log('Subdir');")
                .build());

        // List entries at root level (should show 2 files and 1 directory)
        var rootFiles = snippetService.listFilesByPath("testuser", "/");
        assertTrue(rootFiles.size() >= 3);

        // Verify we have files and directories
        var files = rootFiles.stream()
                .filter(file -> !file.isDirectory())
                .toList();
        var directories = rootFiles.stream()
                .filter(SnippetFile::isDirectory)
                .toList();

        // Should have at least 2 direct files
        assertTrue(files.size() >= 2);
        var fileIds = files.stream().map(SnippetFile::getId).toList();
        assertTrue(fileIds.contains(snippet1.getId()));
        assertTrue(fileIds.contains(snippet2.getId()));

        // Should have at least 1 directory (subdir/)
        assertTrue(directories.size() >= 1);
        var dirPaths = directories.stream().map(SnippetFile::getPath).toList();
        assertTrue(dirPaths.contains("/subdir/"));
    }
}
