from datetime import timedelta

from temporalio import workflow
from temporalio.common import RetryPolicy

# Import activity definitions
with workflow.unsafe.imports_passed_through():
    from activities.chunk_code import chunk_code
    from activities.embed_text import embed_text, embed_texts
    from activities.index_opensearch import index_snippet_to_opensearch
    from activities.summarize_chunks import summarize_chunks
    from activities.summarize_code import summarize_code


@workflow.defn
class SnippetIngestionWorkflow:
    """
    Temporal workflow for ingesting code snippets into the search engine.

    This workflow:
    1. Generates an overall summary of the code
    2. Creates an embedding for the overall summary
    3. Chunks the code semantically
    4. Generates summaries for each chunk
    5. Creates embeddings for all chunk summaries
    6. Indexes everything into OpenSearch
    """

    @workflow.run
    async def run(self, snippet_id: str, code: str) -> dict:
        """
        Execute the snippet ingestion workflow.

        Args:
            snippet_id: Unique identifier for the snippet
            code: The source code to process

        Returns:
            Dictionary containing the ingestion result
        """
        workflow.logger.info(f"Starting ingestion workflow for snippet {snippet_id}")

        # Step 1: Generate overall summary
        workflow.logger.info("Generating overall summary...")
        overall_summary = await workflow.execute_activity(
            summarize_code,
            code,
            start_to_close_timeout=timedelta(seconds=60),
            retry_policy=RetryPolicy(
                maximum_attempts=3,
                initial_interval=timedelta(seconds=1),
                maximum_interval=timedelta(seconds=10),
            ),
        )

        # Step 2: Generate embedding for overall summary
        workflow.logger.info("Generating embedding for overall summary...")
        overall_embedding = await workflow.execute_activity(
            embed_text,
            overall_summary,
            start_to_close_timeout=timedelta(seconds=30),
            retry_policy=RetryPolicy(
                maximum_attempts=3,
                initial_interval=timedelta(seconds=1),
                maximum_interval=timedelta(seconds=10),
            ),
        )

        # Step 3: Chunk the code
        workflow.logger.info("Chunking code...")
        chunks = await workflow.execute_activity(
            chunk_code,
            code,
            start_to_close_timeout=timedelta(seconds=30),
        )

        workflow.logger.info(f"Created {len(chunks)} chunks")

        # Step 4: Generate summaries for each chunk
        workflow.logger.info("Generating chunk summaries...")
        chunk_summaries = await workflow.execute_activity(
            summarize_chunks,
            chunks,
            start_to_close_timeout=timedelta(seconds=120),
            retry_policy=RetryPolicy(
                maximum_attempts=3,
                initial_interval=timedelta(seconds=1),
                maximum_interval=timedelta(seconds=10),
            ),
        )

        # Step 5: Generate embeddings for all chunk summaries
        workflow.logger.info("Generating embeddings for chunk summaries...")
        chunk_embeddings = await workflow.execute_activity(
            embed_texts,
            chunk_summaries,
            start_to_close_timeout=timedelta(seconds=60),
            retry_policy=RetryPolicy(
                maximum_attempts=3,
                initial_interval=timedelta(seconds=1),
                maximum_interval=timedelta(seconds=10),
            ),
        )

        # Step 6: Index to OpenSearch
        workflow.logger.info("Indexing to OpenSearch...")

        # Build a single payload matching the SnippetDocument shape expected by the
        # `index_snippet_to_opensearch` activity. Each chunk becomes an object with
        # an index, code, summary and embedding.
        chunks_payload = []
        for i, (chunk, summary, embedding) in enumerate(
            zip(chunks, chunk_summaries, chunk_embeddings)
        ):
            chunks_payload.append(
                {
                    "chunk_index": i,
                    "code": chunk,
                    "summary": summary,
                    "embedding": embedding,
                }
            )

        snippet_payload = {
            "snippet_id": snippet_id,
            "code": code,
            "overall_summary": overall_summary,
            "overall_embedding": overall_embedding,
            "chunks": chunks_payload,
        }

        result = await workflow.execute_activity(
            index_snippet_to_opensearch,
            snippet_payload,
            start_to_close_timeout=timedelta(seconds=60),
            retry_policy=RetryPolicy(
                maximum_attempts=3,
                initial_interval=timedelta(seconds=1),
                maximum_interval=timedelta(seconds=10),
            ),
        )

        workflow.logger.info(f"Successfully indexed snippet {snippet_id} to OpenSearch")

        return {
            "snippet_id": snippet_id,
            "overall_summary": overall_summary,
            "chunks_count": len(chunks),
            "opensearch_result": result,
        }
