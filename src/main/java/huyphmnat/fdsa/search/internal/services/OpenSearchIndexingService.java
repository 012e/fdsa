package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.FieldNames;
import huyphmnat.fdsa.search.internal.models.CodeFileDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface OpenSearchIndexingService {
    void indexCodeFile(CodeFileDocument document);

    void bulkIndexCodeFiles(List<CodeFileDocument> documents);

    default Map<String, Object> buildCodeFileDocumentMap(CodeFileDocument document) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put(FieldNames.ID, document.getId());
        docMap.put(FieldNames.REPOSITORY_ID, document.getRepositoryId().toString());
        docMap.put(FieldNames.REPOSITORY_IDENTIFIER, document.getRepositoryIdentifier());
        docMap.put(FieldNames.FILE_PATH, document.getFilePath());
        docMap.put(FieldNames.FILE_NAME, document.getFileName());
        docMap.put(FieldNames.FILE_EXTENSION, document.getFileExtension());
        docMap.put(FieldNames.LANGUAGE, document.getLanguage());
        docMap.put(FieldNames.CONTENT, document.getContent());
        docMap.put(FieldNames.SIZE, document.getSize());
        docMap.put(FieldNames.CREATED_AT, document.getCreatedAt().toString());
        docMap.put(FieldNames.UPDATED_AT, document.getUpdatedAt().toString());

        if (document.getChunks() != null && !document.getChunks().isEmpty()) {
            List<Map<String, Object>> chunksData = new ArrayList<>();
            for (CodeFileDocument.CodeChunk chunk : document.getChunks()) {
                Map<String, Object> chunkMap = new HashMap<>();
                chunkMap.put(FieldNames.CHUNK_INDEX, chunk.getIndex());
                chunkMap.put(FieldNames.CHUNK_CONTENT, chunk.getContent());
                chunkMap.put(FieldNames.CHUNK_START_LINE, chunk.getStartLine());
                chunkMap.put(FieldNames.CHUNK_END_LINE, chunk.getEndLine());
                chunksData.add(chunkMap);
            }
            docMap.put(FieldNames.CHUNKS, chunksData);
        }

        return docMap;
    }

    record ChunkData(int index, String code, String summary, List<Double> embedding) {
    }
}
