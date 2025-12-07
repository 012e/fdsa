package huyphmnat.fdsa.snippet.internal.services;

import huyphmnat.fdsa.snippet.dtos.CreateSnippetRequest;
import huyphmnat.fdsa.snippet.dtos.Snippet;
import huyphmnat.fdsa.snippet.exceptions.SnippetNotFoundException;
import huyphmnat.fdsa.snippet.interfaces.SnippetService;
import huyphmnat.fdsa.snippet.internal.entites.SnippetEntity;
import huyphmnat.fdsa.snippet.internal.repositories.SnippetRepository;
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

    @Transactional
    public Snippet createSnippet(CreateSnippetRequest request) {
        var dto = mapper.map(request, SnippetEntity.class);
        snippetRepository.save(dto);
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
    public void deleteSnippet(UUID id) {
        snippetRepository.deleteById(id);
    }
}
