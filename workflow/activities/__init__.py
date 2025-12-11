"""
Activities for the snippet ingestion workflow.

This package contains all Temporal activities used in the workflow:
- summarize_code: Generate overall code summary
- chunk_code: Semantically chunk code
- summarize_chunks: Summarize each code chunk
- embed_text: Generate embedding for text
- embed_texts: Generate embeddings for multiple texts
- index_snippet_to_opensearch: Index snippet to OpenSearch
"""

from .summarize_code import summarize_code
from .chunk_code import chunk_code
from .summarize_chunks import summarize_chunks
from .embed_text import embed_text, embed_texts
from .index_opensearch import index_snippet_to_opensearch

__all__ = [
    "summarize_code",
    "chunk_code",
    "summarize_chunks",
    "embed_text",
    "embed_texts",
    "index_snippet_to_opensearch",
]
