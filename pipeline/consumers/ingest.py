import asyncio
import logging

from kafka import KafkaConsumer
from temporalio.client import Client

from config.kafka import KAFKA_CONSUMER_CONFIG, KAFKA_TOPICS
from config.temporal import TEMPORAL_HOST, TEMPORAL_NAMESPACE, TEMPORAL_TASK_QUEUE
from workflows.repository_ingestion import RepositoryIngestionWorkflow

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)


async def start_repository_workflow(temporal_client: Client, owner: str):
    """
    Start a repository ingestion workflow.

    Args:
        temporal_client: Temporal client instance
        owner: Repository owner/identifier
    """
    try:
        workflow_id = f"repository-ingestion-{owner}"

        handle = await temporal_client.start_workflow(
            RepositoryIngestionWorkflow.run,
            args=[owner],
            id=workflow_id,
            task_queue=TEMPORAL_TASK_QUEUE,
        )

        logger.info(f"Started workflow {workflow_id} for repository {owner}")
        return handle
    except Exception as e:
        logger.error(f"Error starting workflow for repository {owner}: {e}")
        raise


async def consume_events():
    """
    Consume Kafka events and trigger workflows.
    """
    # Connect to Temporal
    temporal_client = await Client.connect(TEMPORAL_HOST, namespace=TEMPORAL_NAMESPACE)
    logger.info(f"Connected to Temporal at {TEMPORAL_HOST}")

    # Create Kafka consumer and subscribe to topics
    consumer = KafkaConsumer(**KAFKA_CONSUMER_CONFIG)
    consumer.subscribe(KAFKA_TOPICS)
    logger.info(f"Started Kafka consumer for topics: {KAFKA_TOPICS}")

    try:
        # Use poll in a loop so we don't block the asyncio event loop.
        while True:
            records = consumer.poll(timeout_ms=1000)
            if not records:
                # Yield control to the event loop briefly
                await asyncio.sleep(0.1)
                continue

            for tp, msgs in records.items():
                for message in msgs:
                    try:
                        # message.value is deserialized by the consumer's value_deserializer
                        event_data = message.value
                        topic = getattr(message, "topic", None)

                        if topic == "repository.cloned":
                            identifier = event_data.get("identifier") or event_data.get("id")
                            if identifier:
                                logger.info(f"Received repository.cloned for {identifier}")
                                await start_repository_workflow(temporal_client, identifier)
                            else:
                                logger.warning(f"Invalid repository.cloned data: {event_data}")
                            continue
                        
                        logger.info(f"Ignoring topic {topic}")

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
