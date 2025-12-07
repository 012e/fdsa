package huyphmnat.fdsa.snippet.internal.services;

import huyphmnat.fdsa.snippet.dtos.*;
import huyphmnat.fdsa.snippet.exceptions.SnippetNotFoundException;
import huyphmnat.fdsa.snippet.interfaces.SnippetService;
import huyphmnat.fdsa.snippet.internal.entites.SnippetEntity;
import huyphmnat.fdsa.snippet.internal.repositories.SnippetRepository;
import huyphmnat.fdsa.snippet.internal.services.interfaces.EventService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SnippetServiceImpl implements SnippetService {
    private final SnippetRepository snippetRepository;
    private final ModelMapper mapper;
    private final EventService eventService;

    @Transactional
    public Snippet createSnippet(CreateSnippetRequest request) {
        var dto = mapper.map(request, SnippetEntity.class);
        snippetRepository.save(dto);
        eventService.publish("snippet.created", mapper.map(dto, SnippetCreatedEvent.class));
        return mapper.map(dto, Snippet.class);
    }

    @Transactional
    public Snippet getSnippet(UUID id) {
        var result = snippetRepository
                .findById(id)
                .orElseThrow(SnippetNotFoundException::new);
        return mapper.map(result, Snippet.class);
    }

    @Transactional
    public java.util.List<Snippet> getAllSnippets() {
        return snippetRepository.findAll()
                .stream()
                .map(entity -> mapper.map(entity, Snippet.class))
                .toList();
    }

    @Transactional
    public Snippet updateSnippet(UpdateSnippetRequest request) {
        var entity = snippetRepository
                .findById(request.getId())
                .orElseThrow(SnippetNotFoundException::new);

        entity.setCode(request.getCode());
        snippetRepository.save(entity);

        eventService.publish("snippet.updated", mapper.map(entity, SnippetUpdatedEvent.class));
        return mapper.map(entity, Snippet.class);
    }

    @Transactional
    public void deleteSnippet(UUID id) {
        var entity = snippetRepository
                .findById(id)
                .orElseThrow(SnippetNotFoundException::new);

        snippetRepository.deleteById(id);
        eventService.publish("snippet.deleted", SnippetDeletedEvent.builder().id(id).build());
    }
}
