package huyphmnat.fdsa.snippet;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.snippet.dtos.*;
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
                .build());
        var createdSnippet = snippetService.getSnippet(newSnippet.getId());

        assertNotNull(createdSnippet);
        assertEquals(newSnippet.getCode(), createdSnippet.getCode());
        assertEquals(newSnippet.getId(), createdSnippet.getId());

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
                .build());

        var retrievedSnippet = snippetService.getSnippet(newSnippet.getId());

        assertNotNull(retrievedSnippet);
        assertEquals(newSnippet.getId(), retrievedSnippet.getId());
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
                .code("console.log('First');")
                .build());

        var snippet2 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
                .code("console.log('Second');")
                .build());

        var snippet3 = snippetService.createSnippet(CreateSnippetRequest
                .builder()
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
                .code("console.log('Original');")
                .build());

        // Reset mock to clear previous invocations
        reset(eventService);

        // Update the snippet
        var updatedSnippet = snippetService.updateSnippet(UpdateSnippetRequest
                .builder()
                .id(newSnippet.getId())
                .code("console.log('Updated');")
                .build());

        assertNotNull(updatedSnippet);
        assertEquals(newSnippet.getId(), updatedSnippet.getId());
        assertEquals("console.log('Updated');", updatedSnippet.getCode());

        // Verify the snippet was updated in database
        var retrievedSnippet = snippetService.getSnippet(newSnippet.getId());
        assertEquals("console.log('Updated');", retrievedSnippet.getCode());

        // Verify that the update event was published correctly
        verify(eventService, times(1)).publish(eventNameCaptor.capture(), eventPayloadCaptor.capture());

        assertEquals("snippet.updated", eventNameCaptor.getValue());

        Object payload = eventPayloadCaptor.getValue();
        assertInstanceOf(SnippetUpdatedEvent.class, payload);

        SnippetUpdatedEvent event = (SnippetUpdatedEvent) payload;
        assertEquals(updatedSnippet.getId(), event.getId());
        assertEquals(updatedSnippet.getCode(), event.getCode());
    }

    @Test
    public void testUpdateSnippet_NotFound() {
        var randomId = java.util.UUID.randomUUID();
        assertThrows(SnippetNotFoundException.class, () -> {
            snippetService.updateSnippet(UpdateSnippetRequest
                    .builder()
                    .id(randomId)
                    .code("console.log('Will Fail');")
                    .build());
        });
    }

    @Test
    public void testDeleteSnippet() {
        var newSnippet = snippetService.createSnippet(CreateSnippetRequest
                .builder()
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
}
