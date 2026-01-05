package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.internal.models.CodeFileDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface OpenSearchIndexingService {
    void indexSnippet(
            String snippetId,
            String code,
            String overallSummary,
            List<Double> overallEmbedding,
            List<ChunkData> chunks);

    void indexCodeFile(CodeFileDocument document);

    void bulkIndexCodeFiles(List<CodeFileDocument> documents);

    default Map<String, Object> buildCodeFileDocumentMap(CodeFileDocument document) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("id", document.getId());
        docMap.put("repository_id", document.getRepositoryId().toString());
        docMap.put("repository_identifier", document.getRepositoryIdentifier());
        docMap.put("file_path", document.getFilePath());
        docMap.put("file_name", document.getFileName());
        docMap.put("file_extension", document.getFileExtension());
        docMap.put("language", document.getLanguage());
        docMap.put("content", document.getContent());
        docMap.put("size", document.getSize());
        docMap.put("created_at", document.getCreatedAt().toString());
        docMap.put("updated_at", document.getUpdatedAt().toString());

        if (document.getChunks() != null && !document.getChunks().isEmpty()) {
            List<Map<String, Object>> chunksData = new ArrayList<>();
            for (CodeFileDocument.CodeChunk chunk : document.getChunks()) {
                Map<String, Object> chunkMap = new HashMap<>();
                chunkMap.put("index", chunk.getIndex());
                chunkMap.put("content", chunk.getContent());
                chunkMap.put("start_line", chunk.getStartLine());
                chunkMap.put("end_line", chunk.getEndLine());
                chunksData.add(chunkMap);
            }
            docMap.put("chunks", chunksData);
        }

        return docMap;
    }

    public record ChunkData(int index, String code, String summary, List<Double> embedding) {
    }
}
