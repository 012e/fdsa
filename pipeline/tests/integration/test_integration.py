"""test_int
Integration test for snippet ingestion workflow using Testcontainers.

This test:
1. Starts OpenSearch container
2. Uses the application's Temporal instance (from docker-compose)
3. Starts a Temporal worker
4. Executes the workflow with sample code
5. Verifies the document exists in OpenSearch with all expected fields

Requirements:
- pytest
- pytest-asyncio
- testcontainers[opensearch]
- Temporal must be running (via docker-compose)

Run:
    # Make sure Temporal is running first
    docker compose up -d temporal

    # Auto-discovery mode (recommended)
    pytest -v -s

    # Specific test file
    pytest test_integration.py -v -s

    # Using just
    just test

    # Run integration tests only
    pytest -m integration -v -s
    just test-integration

    # Skip slow tests
    pytest -m "not slow" -v
    just test-fast
"""

import asyncio
import os

import pytest
from opensearchpy import OpenSearch
from temporalio.client import Client
from temporalio.worker import Worker
from testcontainers.opensearch import OpenSearchContainer

# Import workflow and activities
from activities.chunk_code import chunk_code
from activities.embed_text import embed_text, embed_texts
from activities.index_opensearch import index_snippet_to_opensearch
from activities.summarize_chunks import summarize_chunks
from activities.summarize_code import summarize_code
from workflows.snippet_ingestion import SnippetIngestionWorkflow

# Sample code for testing
SAMPLE_CODE = """
def fibonacci(n):
    \"\"\"Calculate the nth Fibonacci number using recursion.\"\"\"
    if n <= 1:
        return n
    return fibonacci(n-1) + fibonacci(n-2)

def factorial(n):
    \"\"\"Calculate factorial of n.\"\"\"
    if n <= 1:
        return 1
    return n * factorial(n-1)

def main():
    print("Fibonacci sequence:")
    for i in range(10):
        print(f"fib({i}) = {fibonacci(i)}")
    
    print("\\nFactorials:")
    for i in range(1, 6):
        print(f"{i}! = {factorial(i)}")

if __name__ == "__main__":
    main()
"""

SNIPPET_ID = "test-integration-snippet-001"
TEMPORAL_NAMESPACE = "default"
TEMPORAL_TASK_QUEUE = "test-snippet-ingestion-queue"
TEMPORAL_HOST = os.getenv("TEMPORAL_HOST", "localhost:7233")


@pytest.fixture(scope="module")
def opensearch_container():
    """Start OpenSearch container for testing."""
    # Use the same image as docker-compose
    container = OpenSearchContainer(image="opensearchproject/opensearch:3.3.0")
    # Disable security like in docker-compose
    container.with_env("plugins.security.disabled", "true")
    container.with_env("OPENSEARCH_INITIAL_ADMIN_PASSWORD", "Demo@123")
    container.with_env("discovery.type", "single-node")

    with container:
        yield container


@pytest.fixture(scope="module")
def opensearch_client(opensearch_container):
    """Create OpenSearch client connected to the test container."""
    host = opensearch_container.get_container_host_ip()
    port = opensearch_container.get_exposed_port(9200)

    client = OpenSearch(
        hosts=[{"host": host, "port": port}],
        http_compress=True,
        use_ssl=False,
        verify_certs=False,
        ssl_assert_hostname=False,
        ssl_show_warn=False,
    )

    # Wait for OpenSearch to be ready
    for _ in range(30):
        try:
            if client.ping():
                break
        except Exception:
            pass
        asyncio.run(asyncio.sleep(1))

    return client


@pytest.fixture(scope="module")
def temporal_client():
    """Create Temporal client connected to the application's Temporal instance."""

    async def _create_client():
        try:
            client = await Client.connect(TEMPORAL_HOST, namespace=TEMPORAL_NAMESPACE)
            return client
        except Exception as e:
            pytest.fail(
                f"Failed to connect to Temporal at {TEMPORAL_HOST}. "
                f"Make sure Temporal is running (docker compose up -d temporal). "
                f"Error: {e}"
            )

    return asyncio.run(_create_client())


@pytest.fixture
def setup_environment(opensearch_container):
    """Set up environment variables for the test."""
    # Set OpenSearch connection details
    host = opensearch_container.get_container_host_ip()
    port = opensearch_container.get_exposed_port(9200)

    os.environ["OPENSEARCH_HOST"] = host
    os.environ["OPENSEARCH_PORT"] = str(port)
    os.environ["OPENSEARCH_INDEX"] = "test_code_snippets"

    # Ensure OPENAI_API_KEY is set (should be in .env)
    if not os.getenv("OPENAI_API_KEY"):
        pytest.skip("OPENAI_API_KEY not set in environment")

    yield

    # Cleanup
    os.environ.pop("OPENSEARCH_HOST", None)
    os.environ.pop("OPENSEARCH_PORT", None)
    os.environ.pop("OPENSEARCH_INDEX", None)


async def run_worker(client: Client, stop_event: asyncio.Event):
    """Run a Temporal worker for the test."""
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

    # Run worker until stop event is set
    async def worker_wrapper():
        try:
            await worker.run()
        except asyncio.CancelledError:
            pass

    worker_task = asyncio.create_task(worker_wrapper())

    # Wait for stop event
    await stop_event.wait()

    # Cancel worker
    worker_task.cancel()
    try:
        await worker_task
    except asyncio.CancelledError:
        pass


@pytest.mark.asyncio
@pytest.mark.integration
@pytest.mark.slow
async def test_snippet_ingestion_workflow(
    temporal_client, opensearch_client, setup_environment
):
    """
    Integration test for the complete snippet ingestion workflow.

    This test verifies that:
    1. The workflow executes successfully
    2. The document is indexed in OpenSearch
    3. All expected fields are present
    4. Chunks are properly created and embedded
    """
    print("\n" + "=" * 70)
    print("Starting Integration Test: Snippet Ingestion Workflow")
    print("=" * 70)

    # Start worker in background
    stop_event = asyncio.Event()
    worker_task = asyncio.create_task(run_worker(temporal_client, stop_event))

    # Give worker time to start
    await asyncio.sleep(2)

    try:
        print(f"\n📝 Testing with snippet ID: {SNIPPET_ID}")
        print(f"Code length: {len(SAMPLE_CODE)} characters")

        # Start the workflow
        print("\n🚀 Starting workflow...")
        handle = await temporal_client.start_workflow(
            SnippetIngestionWorkflow.run,
            args=[SNIPPET_ID, SAMPLE_CODE],
            id=f"test-workflow-{SNIPPET_ID}",
            task_queue=TEMPORAL_TASK_QUEUE,
        )

        print(f"✅ Workflow started: {handle.id}")

        # Wait for workflow to complete
        print("\n⏳ Waiting for workflow to complete...")
        result = await asyncio.wait_for(
            handle.result(),
            timeout=180.0,  # 3 minutes timeout
        )

        print("\n✅ Workflow completed successfully!")
        print(f"Result: {result}")

        # Verify result structure
        assert "snippet_id" in result, "Result missing snippet_id"
        assert "chunks_count" in result, "Result missing chunks_count"
        assert "overall_summary" in result, "Result missing overall_summary"
        assert "opensearch_result" in result, "Result missing opensearch_result"

        assert result["snippet_id"] == SNIPPET_ID
        assert result["chunks_count"] > 0, "No chunks were created"
        assert len(result["overall_summary"]) > 0, "Overall summary is empty"

        print("\n📊 Workflow Results:")
        print(f"  - Snippet ID: {result['snippet_id']}")
        print(f"  - Chunks created: {result['chunks_count']}")
        print(f"  - Summary length: {len(result['overall_summary'])} chars")
        print(f"  - OpenSearch result: {result['opensearch_result']}")

        # Verify document exists in OpenSearch
        print("\n🔍 Verifying document in OpenSearch...")

        # Refresh index to ensure document is searchable
        opensearch_client.indices.refresh(index="test_code_snippets")

        # Get the document
        doc_response = opensearch_client.get(index="test_code_snippets", id=SNIPPET_ID)

        assert doc_response["found"], "Document not found in OpenSearch"

        doc = doc_response["_source"]

        print("✅ Document found in OpenSearch!")

        # Verify document structure
        print("\n🔬 Verifying document structure...")

        # Check required fields
        required_fields = [
            "snippet_id",
            "code",
            "overall_summary",
            "overall_embedding",
            "chunks",
            "created_at",
            "updated_at",
        ]

        for field in required_fields:
            assert field in doc, f"Document missing field: {field}"
            print(f"  ✓ Field '{field}' present")

        # Verify field values
        assert doc["snippet_id"] == SNIPPET_ID
        assert doc["code"] == SAMPLE_CODE
        assert len(doc["overall_summary"]) > 0
        assert len(doc["overall_embedding"]) == 1024, (
            f"Expected 1024-dim embedding, got {len(doc['overall_embedding'])}"
        )

        # Verify chunks
        chunks = doc["chunks"]
        assert len(chunks) > 0, "No chunks in document"
        assert len(chunks) == result["chunks_count"]

        print("\n📦 Chunk Verification:")
        print(f"  - Total chunks: {len(chunks)}")

        for i, chunk in enumerate(chunks):
            # Verify chunk structure
            assert "chunk_index" in chunk
            assert "code" in chunk
            assert "summary" in chunk
            assert "embedding" in chunk

            # Verify chunk values
            assert chunk["chunk_index"] == i
            assert len(chunk["code"]) > 0
            assert len(chunk["summary"]) > 0
            assert len(chunk["embedding"]) == 1024, (
                f"Chunk {i} has wrong embedding dimension"
            )

            print(
                f"  ✓ Chunk {i}: {len(chunk['code'])} chars code, "
                f"{len(chunk['summary'])} chars summary"
            )

        # Test semantic search capability
        print("\n🔍 Testing semantic search...")

        # Simple search query
        search_query = {"query": {"match": {"overall_summary": "fibonacci"}}}

        search_response = opensearch_client.search(
            index="test_code_snippets", body=search_query
        )

        assert search_response["hits"]["total"]["value"] > 0, (
            "Document not found in search"
        )

        print(
            f"  ✓ Document searchable (found {search_response['hits']['total']['value']} results)"
        )

        print("\n" + "=" * 70)
        print("✅ ALL TESTS PASSED!")
        print("=" * 70)

    finally:
        # Stop worker
        stop_event.set()
        await worker_task


@pytest.mark.asyncio
async def test_opensearch_connection(opensearch_client):
    """Test that OpenSearch container is properly connected."""
    info = opensearch_client.info()
    print(f"\nOpenSearch version: {info['version']['number']}")
    assert opensearch_client.ping()


@pytest.mark.asyncio
async def test_temporal_connection(temporal_client):
    """Test that Temporal application instance is properly connected."""
    # Simply verify we can interact with the client
    assert temporal_client is not None
    print(
        f"\nTemporal client connected to application instance: {temporal_client.service_client.config.target_host}"
    )


if __name__ == "__main__":
    # Run with pytest
    import sys

    sys.exit(pytest.main([__file__, "-v", "-s"]))
