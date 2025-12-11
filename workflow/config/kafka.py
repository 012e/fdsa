import os
from typing import Any

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "snippet-ingestion-worker")
KAFKA_TOPICS = ["snippet.created"]

# Kafka consumer configuration
KAFKA_CONSUMER_CONFIG = {
    "bootstrap_servers": KAFKA_BOOTSTRAP_SERVERS.split(","),
    "group_id": KAFKA_GROUP_ID,
    "auto_offset_reset": "earliest",
    "enable_auto_commit": True,
    "value_deserializer": lambda m: m.decode("utf-8") if m else None,
}
