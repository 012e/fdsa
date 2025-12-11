import asyncio
from temporalio.client import Client
from temporalio.worker import Worker
from config.temporal import TEMPORAL_HOST, TEMPORAL_NAMESPACE, TEMPORAL_TASK_QUEUE
from workflows.snippet_ingestion import SnippetIngestionWorkflow
from activities.summarize_code import summarize_code
from activities.chunk_code import chunk_code
from activities.summarize_chunks import summarize_chunks
from activities.embed_text import embed_text, embed_texts
from activities.index_opensearch import index_snippet_to_opensearch
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def main():
    """
    Start the Temporal worker that executes activities and workflows.
    """
    # Connect to Temporal
    client = await Client.connect(TEMPORAL_HOST, namespace=TEMPORAL_NAMESPACE)
    logger.info(f"Connected to Temporal at {TEMPORAL_HOST}")
    
    # Create worker
    worker = Worker(
        client,
        task_queue=TEMPORAL_TASK_QUEUE,
        workflows=[SnippetIngestionWorkflow],
        activities=[
            summarize_code,
            chunk_code,
            summarize_chunks,
            embed_text,
            embed_texts,
            index_snippet_to_opensearch,
        ],
    )
    
    logger.info(f"Starting worker on task queue: {TEMPORAL_TASK_QUEUE}")
    await worker.run()


if __name__ == "__main__":
    asyncio.run(main())
