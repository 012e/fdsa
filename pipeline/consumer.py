import asyncio
import json
from kafka import KafkaConsumer
from temporalio.client import Client
from config.kafka import KAFKA_CONSUMER_CONFIG, KAFKA_TOPICS
from config.temporal import TEMPORAL_HOST, TEMPORAL_NAMESPACE, TEMPORAL_TASK_QUEUE
from workflows.snippet_ingestion import SnippetIngestionWorkflow
import logging

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def start_workflow(temporal_client: Client, snippet_id: str, code: str):
    """
    Start a snippet ingestion workflow.
    
    Args:
        temporal_client: Temporal client instance
        snippet_id: Unique identifier for the snippet
        code: Source code to process
    """
    try:
        workflow_id = f"snippet-ingestion-{snippet_id}"
        
        handle = await temporal_client.start_workflow(
            SnippetIngestionWorkflow.run,
            args=[snippet_id, code],
            id=workflow_id,
            task_queue=TEMPORAL_TASK_QUEUE,
        )
        
        logger.info(f"Started workflow {workflow_id} for snippet {snippet_id}")
        return handle
    except Exception as e:
        logger.error(f"Error starting workflow for snippet {snippet_id}: {e}")
        raise


async def consume_events():
    """
    Consume Kafka events and trigger workflows.
    """
    # Connect to Temporal
    temporal_client = await Client.connect(TEMPORAL_HOST, namespace=TEMPORAL_NAMESPACE)
    logger.info(f"Connected to Temporal at {TEMPORAL_HOST}")
    
    # Create Kafka consumer
    consumer = KafkaConsumer(*KAFKA_TOPICS, **KAFKA_CONSUMER_CONFIG)
    logger.info(f"Started Kafka consumer for topics: {KAFKA_TOPICS}")
    
    try:
        for message in consumer:
            try:
                # Parse the message
                event_data = json.loads(message.value)
                snippet_id = event_data.get("id")
                code = event_data.get("code")
                
                if not snippet_id or not code:
                    logger.warning(f"Invalid event data: {event_data}")
                    continue
                
                logger.info(f"Received snippet.created event for snippet {snippet_id}")
                
                # Start the workflow
                await start_workflow(temporal_client, str(snippet_id), code)
                
            except json.JSONDecodeError as e:
                logger.error(f"Failed to parse message: {e}")
            except Exception as e:
                logger.error(f"Error processing message: {e}")
    finally:
        consumer.close()
        logger.info("Kafka consumer closed")


def main():
    """Main entry point for the Kafka consumer."""
    logger.info("Starting Kafka consumer for snippet ingestion...")
    asyncio.run(consume_events())


if __name__ == "__main__":
    main()
