from temporalio import activity
from opensearchpy import OpenSearch
from typing import Any
import os
from datetime import datetime


def get_opensearch_client() -> OpenSearch:
    """Create and return an OpenSearch client."""
    host = os.getenv("OPENSEARCH_HOST", "localhost")
    port = int(os.getenv("OPENSEARCH_PORT", "9200"))
    
    # For development without SSL
    client = OpenSearch(
        hosts=[{"host": host, "port": port}],
        http_compress=True,
        use_ssl=False,
        verify_certs=False,
        ssl_assert_hostname=False,
        ssl_show_warn=False,
    )
    return client


@activity.defn
async def index_snippet_to_opensearch(
    snippet_id: str,
    code: str,
    overall_summary: str,
    overall_embedding: list[float],
    chunks: list[str],
    chunk_summaries: list[str],
    chunk_embeddings: list[list[float]],
) -> dict[str, Any]:
    """
    Activity to index the snippet and its chunks into OpenSearch.
    
    Args:
        snippet_id: Unique identifier for the snippet
        code: Original source code
        overall_summary: Summary of the entire code
        overall_embedding: Embedding vector for the overall summary
        chunks: List of code chunks
        chunk_summaries: List of summaries for each chunk
        chunk_embeddings: List of embedding vectors for each chunk
        
    Returns:
        Result information including document IDs
    """
    client = get_opensearch_client()
    index_name = "code_snippets"
    
    # Create index if it doesn't exist
    if not client.indices.exists(index=index_name):
        index_body = {
            "settings": {
                "index": {
                    "number_of_shards": 1,
                    "number_of_replicas": 1,
                    "knn": True,
                }
            },
            "mappings": {
                "properties": {
                    "snippet_id": {"type": "keyword"},
                    "code": {"type": "text"},
                    "overall_summary": {"type": "text"},
                    "overall_embedding": {
                        "type": "knn_vector",
                        "dimension": 1024,
                        "method": {
                            "name": "hnsw",
                            "space_type": "cosinesimil",
                            "engine": "nmslib",
                        }
                    },
                    "chunks": {
                        "type": "nested",
                        "properties": {
                            "chunk_index": {"type": "integer"},
                            "code": {"type": "text"},
                            "summary": {"type": "text"},
                            "embedding": {
                                "type": "knn_vector",
                                "dimension": 1024,
                                "method": {
                                    "name": "hnsw",
                                    "space_type": "cosinesimil",
                                    "engine": "nmslib",
                                }
                            }
                        }
                    },
                    "created_at": {"type": "date"},
                    "updated_at": {"type": "date"},
                }
            }
        }
        client.indices.create(index=index_name, body=index_body)
    
    # Prepare chunks data
    chunks_data = []
    for idx, (chunk, summary, embedding) in enumerate(zip(chunks, chunk_summaries, chunk_embeddings)):
        chunks_data.append({
            "chunk_index": idx,
            "code": chunk,
            "summary": summary,
            "embedding": embedding,
        })
    
    # Prepare document
    document = {
        "snippet_id": snippet_id,
        "code": code,
        "overall_summary": overall_summary,
        "overall_embedding": overall_embedding,
        "chunks": chunks_data,
        "created_at": datetime.utcnow().isoformat(),
        "updated_at": datetime.utcnow().isoformat(),
    }
    
    # Index the document
    response = client.index(
        index=index_name,
        body=document,
        id=snippet_id,
    )
    
    # Refresh index to make document immediately searchable
    client.indices.refresh(index=index_name)
    
    return {
        "index": index_name,
        "document_id": snippet_id,
        "result": response["result"],
        "chunks_count": len(chunks_data),
    }
