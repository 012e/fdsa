import os

KAFKA_BOOTSTRAP_SERVERS = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
KAFKA_GROUP_ID = os.getenv("KAFKA_GROUP_ID", "snippet-ingestion-worker")
KAFKA_TOPICS = ["snippet.created", "snippet.updated"]

# Kafka consumer configuration
import json
from typing import Any

# Use a JSON deserializer by default so consumers receive dicts
def _json_deserializer(m: bytes) -> Any:
    if not m:
        return None
    try:
        return json.loads(m.decode("utf-8"))
    except Exception:
        # Fall back to returning the raw decoded string on failure
        return m.decode("utf-8")

KAFKA_CONSUMER_CONFIG = {
    "bootstrap_servers": KAFKA_BOOTSTRAP_SERVERS.split(","),
    "group_id": KAFKA_GROUP_ID,
    "auto_offset_reset": "earliest",
    "enable_auto_commit": True,
    "value_deserializer": _json_deserializer,
}
