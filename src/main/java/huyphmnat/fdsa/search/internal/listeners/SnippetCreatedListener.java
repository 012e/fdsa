package huyphmnat.fdsa.search.internal.listeners;

import huyphmnat.fdsa.search.internal.services.interfaces.SearchIndexService;
import huyphmnat.fdsa.snippet.dtos.SnippetCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
@RequiredArgsConstructor
@Slf4j
public class SnippetCreatedListener {
    private final SearchIndexService searchIndexService;
    private final ModelMapper modelMapper;
    @KafkaListener(
            topics= "snippet.created",
            groupId = "search-service"
    )
    public void listen(SnippetCreatedEvent event) {
        var dto = modelMapper.map(event, huyphmnat.fdsa.snippet.dtos.Snippet.class);
        searchIndexService.startIndexingSnippet(dto);
    }
}
