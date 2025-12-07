package huyphmnat.fdsa.search.internal.listeners;

import huyphmnat.fdsa.snippet.dtos.SnippetCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnippetCreatedListener {
    @KafkaListener(
            topics= "snippet.created",
            groupId = "search-service"
    )
    public void listen(SnippetCreatedEvent event) {
        log.info("Received SnippetCreatedEvent: id={}, code={}", event.getId(), event.getCode());
        // TODO: Process the event
    }
}
