"""
Test script to manually trigger a snippet ingestion workflow.

This script allows you to test the workflow without needing to publish
a Kafka event. It directly starts a Temporal workflow.

Usage:
    python test_workflow.py
"""

import asyncio
import sys
from temporalio.client import Client
from config.temporal import TEMPORAL_HOST, TEMPORAL_NAMESPACE, TEMPORAL_TASK_QUEUE
from workflows.snippet_ingestion import SnippetIngestionWorkflow


async def test_workflow():
    """Test the snippet ingestion workflow with sample data."""
    
    # Sample code to test
    sample_code = """
def fibonacci(n):
    \"\"\"Calculate the nth Fibonacci number.\"\"\"
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)

def main():
    for i in range(10):
        print(f"fib({i}) = {fibonacci(i)}")

if __name__ == "__main__":
    main()
"""
    
    snippet_id = "test-snippet-12345"
    
    print(f"Connecting to Temporal at {TEMPORAL_HOST}...")
    client = await Client.connect(TEMPORAL_HOST, namespace=TEMPORAL_NAMESPACE)
    
    print(f"\nStarting workflow for snippet: {snippet_id}")
    print(f"Task queue: {TEMPORAL_TASK_QUEUE}")
    print(f"\nCode:\n{sample_code}\n")
    
    try:
        # Start the workflow
        handle = await client.start_workflow(
            SnippetIngestionWorkflow.run,
            args=[snippet_id, sample_code],
            id=f"test-workflow-{snippet_id}",
            task_queue=TEMPORAL_TASK_QUEUE,
        )
        
        print(f"Workflow started! ID: {handle.id}")
        print(f"Workflow URL: http://localhost:8233/namespaces/{TEMPORAL_NAMESPACE}/workflows/{handle.id}")
        print("\nWaiting for workflow to complete...")
        
        # Wait for result
        result = await handle.result()
        
        print("\n✅ Workflow completed successfully!")
        print(f"\nResults:")
        print(f"  Snippet ID: {result['snippet_id']}")
        print(f"  Chunks created: {result['chunks_count']}")
        print(f"  Overall summary: {result['overall_summary'][:200]}...")
        print(f"\n  OpenSearch result: {result['opensearch_result']}")
        
    except Exception as e:
        print(f"\n❌ Workflow failed: {e}")
        sys.exit(1)


def main():
    """Run the test workflow."""
    print("=" * 70)
    print("Testing Snippet Ingestion Workflow")
    print("=" * 70)
    print("\nMake sure:")
    print("1. Docker services are running (docker compose up -d)")
    print("2. Worker is running (python worker.py)")
    print("3. .env file is configured with OPENAI_API_KEY")
    print("\n" + "=" * 70 + "\n")
    
    input("Press Enter to start the test workflow...")
    
    asyncio.run(test_workflow())


if __name__ == "__main__":
    main()
