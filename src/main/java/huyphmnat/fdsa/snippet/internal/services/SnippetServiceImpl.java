package huyphmnat.fdsa.snippet.internal.services;

import huyphmnat.fdsa.snippet.dtos.*;
import huyphmnat.fdsa.snippet.exceptions.DuplicateSnippetPathException;
import huyphmnat.fdsa.snippet.exceptions.SnippetNotFoundException;
import huyphmnat.fdsa.snippet.interfaces.SnippetService;
import huyphmnat.fdsa.snippet.internal.entites.SnippetEntity;
import huyphmnat.fdsa.snippet.internal.repositories.SnippetRepository;
import huyphmnat.fdsa.shared.events.EventService;
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
        if (snippetRepository.existsByOwnerAndPath(request.getOwner(), request.getPath())) {
            throw new DuplicateSnippetPathException(request.getPath());
        }
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
    public Snippet getSnippetByPath(String owner, String path) {
        var result = snippetRepository
                .findByOwnerAndPath(owner, path)
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
    public java.util.List<Snippet> getSnippetsByOwner(String owner) {
        return snippetRepository.findByOwner(owner)
                .stream()
                .map(entity -> mapper.map(entity, Snippet.class))
                .toList();
    }

    @Transactional
    public Snippet updateSnippet(UpdateSnippetRequest request) {
        var entity = snippetRepository
                .findById(request.getId())
                .orElseThrow(SnippetNotFoundException::new);

        // Track if owner or path is being changed
        String newOwner = request.getOwner() != null ? request.getOwner() : entity.getOwner();
        String newPath = request.getPath() != null ? request.getPath() : entity.getPath();

        // Check if owner+path combination is being changed and if it already exists
        boolean ownerOrPathChanged = !newOwner.equals(entity.getOwner()) || !newPath.equals(entity.getPath());
        if (ownerOrPathChanged) {
            if (snippetRepository.existsByOwnerAndPath(newOwner, newPath)) {
                throw new DuplicateSnippetPathException(newPath);
            }
        }

        if (request.getOwner() != null) {
            entity.setOwner(request.getOwner());
        }

        if (request.getPath() != null) {
            entity.setPath(request.getPath());
        }

        if (request.getCode() != null) {
            entity.setCode(request.getCode());
        }
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

    @Transactional
    public java.util.List<SnippetFile> listFilesByPath(String owner, String path) {
        // Normalize path to always end with '/' for directory queries
        String searchPath = path.endsWith("/") ? path : path + "/";

        // Find all snippets that start with this path within the owner's namespace
        var entities = snippetRepository.findByOwnerAndPathStartingWith(owner, searchPath);

        // Track unique entries (files and directories)
        var entriesMap = new java.util.LinkedHashMap<String, SnippetFile>();

        for (var entity : entities) {
            String relativePath = entity.getPath().substring(searchPath.length());

            // Check if this is a direct child or nested
            int slashIndex = relativePath.indexOf('/');

            if (slashIndex == -1) {
                // Direct file - add if not already present
                if (!entriesMap.containsKey(entity.getPath())) {
                    entriesMap.put(entity.getPath(), SnippetFile.builder()
                            .id(entity.getId())
                            .owner(entity.getOwner())
                            .path(entity.getPath())
                            .isDirectory(false)
                            .build());
                }
            } else {
                // Nested path - extract directory name
                String dirName = relativePath.substring(0, slashIndex);
                String dirPath = searchPath + dirName + "/";

                // Add directory entry if not already present
                if (!entriesMap.containsKey(dirPath)) {
                    entriesMap.put(dirPath, SnippetFile.builder()
                            .id(null)  // Directories don't have IDs
                            .owner(owner)
                            .path(dirPath)
                            .isDirectory(true)
                            .build());
                }
            }
        }

        return new java.util.ArrayList<>(entriesMap.values());
    }
}
