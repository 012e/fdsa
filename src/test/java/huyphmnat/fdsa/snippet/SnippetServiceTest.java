package huyphmnat.fdsa.snippet;

import huyphmnat.fdsa.base.BaseIntegrationTest;
import huyphmnat.fdsa.snippet.dtos.CreateSnippetRequest;
import huyphmnat.fdsa.snippet.dtos.SnippetCreatedEvent;
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
