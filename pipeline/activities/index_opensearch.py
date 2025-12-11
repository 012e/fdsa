import os
from datetime import datetime
from typing import Any, List, Optional

from opensearchpy import OpenSearch

# Use pydantic dataclasses for input validation/typing
from pydantic.dataclasses import dataclass as pydantic_dataclass
from temporalio import activity


@pydantic_dataclass
class Chunk:
    chunk_index: int
    code: str
    summary: str
    embedding: List[float]


@pydantic_dataclass
class SnippetDocument:
    snippet_id: str
    code: str
    overall_summary: str
    overall_embedding: List[float]
    chunks: List[Chunk]
    created_at: Optional[datetime] = None
    updated_at: Optional[datetime] = None


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
    snippet: SnippetDocument | dict,
) -> dict[str, Any]:
    """
    Activity to index the snippet and its chunks into OpenSearch.

    Args:
        snippet: A `SnippetDocument` instance or a dict representing the snippet
            containing fields: `snippet_id`, `code`, `overall_summary`,
            `overall_embedding`, and `chunks` (list of chunk dicts or `Chunk`).

    Returns:
        Result information including document IDs
    """
    # Accept either a SnippetDocument instance or a dict that can be parsed into one.
    if not isinstance(snippet, SnippetDocument):
        # snippet may be a dict coming from the activity call - parse it into our dataclass
        # Support both flat chunk dicts and already-parsed Chunk instances
        if isinstance(snippet, dict):
            # Convert nested chunk dicts into Chunk objects if necessary
            raw_chunks = snippet.get("chunks", [])
            parsed_chunks: List[Chunk] = []
            for c in raw_chunks:
                if isinstance(c, Chunk):
                    parsed_chunks.append(c)
                elif isinstance(c, dict):
                    # allow missing chunk_index and compute if absent
                    parsed_chunks.append(Chunk(**c))
                else:
                    raise TypeError(f"Unsupported chunk type: {type(c)}")

            snippet = SnippetDocument(
                snippet_id=snippet["snippet_id"],
                code=snippet["code"],
                overall_summary=snippet["overall_summary"],
                overall_embedding=snippet["overall_embedding"],
                chunks=parsed_chunks,
                created_at=snippet.get("created_at"),
                updated_at=snippet.get("updated_at"),
            )
        else:
            # If it's already an object with the expected attributes, try to coerce
            raise TypeError("snippet must be a dict or SnippetDocument")

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
                            "engine": "faiss",
                        },
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
                                    "engine": "faiss",
                                },
                            },
                        },
                    },
                    "created_at": {"type": "date"},
                    "updated_at": {"type": "date"},
                }
            },
        }
        client.indices.create(index=index_name, body=index_body)

    # Prepare chunks data from the SnippetDocument.chunks
    chunks_data = []
    for c in snippet.chunks:
        chunks_data.append(
            {
                "chunk_index": c.chunk_index,
                "code": c.code,
                "summary": c.summary,
                "embedding": c.embedding,
            }
        )

    # Prepare document
    document = {
        "snippet_id": snippet.snippet_id,
        "code": snippet.code,
        "overall_summary": snippet.overall_summary,
        "overall_embedding": snippet.overall_embedding,
        "chunks": chunks_data,
        "created_at": (
            snippet.created_at.isoformat()
            if snippet.created_at
            else datetime.utcnow().isoformat()
        ),
        "updated_at": (
            snippet.updated_at.isoformat()
            if snippet.updated_at
            else datetime.utcnow().isoformat()
        ),
    }

    # Index the document
    response = client.index(
        index=index_name,
        body=document,
        id=snippet.snippet_id,
    )

    # Refresh index to make document immediately searchable
    client.indices.refresh(index=index_name)

    return {
        "index": index_name,
        "document_id": snippet.snippet_id,
        "result": response["result"],
        "chunks_count": len(chunks_data),
    }
