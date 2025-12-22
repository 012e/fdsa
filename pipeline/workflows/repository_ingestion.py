from datetime import timedelta
from temporalio import workflow
from temporalio.common import RetryPolicy

# Import configuration
from config.temporal import TEMPORAL_TASK_QUEUE

with workflow.unsafe.imports_passed_through():
    from activities.repository_actions import discover_files, fetch_file_content, list_directory
    from workflows.snippet_ingestion import SnippetIngestionWorkflow


@workflow.defn
class RepoFileIngestionWorkflow:
    """
    Workflow to ingest a single file from a repository.
    Fetches content and then triggers SnippetIngestionWorkflow.
    """
    @workflow.run
    async def run(self, owner: str, path: str) -> dict:
        workflow.logger.info(f"Processing file: {path} for owner: {owner}")
        
        # Fetch file content
        code = await workflow.execute_activity(
            fetch_file_content,
            args=[owner, path],
            start_to_close_timeout=timedelta(seconds=30),
            retry_policy=RetryPolicy(
                maximum_attempts=3,
                initial_interval=timedelta(seconds=1),
            ),
        )
        
        # Execute SnippetIngestionWorkflow as a child workflow
        # We use the path as the snippet_id
        result = await workflow.execute_child_workflow(
            SnippetIngestionWorkflow.run,
            args=[path, code],
            id=f"ingest-{owner}-{path.replace('/', '-')}",
            task_queue=TEMPORAL_TASK_QUEUE,
            parent_close_policy=workflow.ParentClosePolicy.ABANDON,
        )
        
        return result


@workflow.defn
class RepositoryIngestionWorkflow:
    """
    Workflow to ingest an entire repository using recursive BFS.
    """
    @workflow.run
    async def run(self, owner: str) -> dict:
        workflow.logger.info(f"Starting recursive repository ingestion for {owner}")
        
        queue = [""]
        visited = set()
        files_processed = 0
        
        while queue:
            current_path = queue.pop(0)
            if current_path in visited:
                continue
            visited.add(current_path)
            
            workflow.logger.info(f"Listing directory: {current_path}")
            
            # List directory
            items = await workflow.execute_activity(
                list_directory,
                args=[owner, current_path],
                start_to_close_timeout=timedelta(seconds=30),
                retry_policy=RetryPolicy(
                    maximum_attempts=3,
                    initial_interval=timedelta(seconds=1),
                ),
            )
            
            for item in items:
                path = item["path"]
                if item["isDirectory"]:
                    if path not in visited:
                        queue.append(path)
                else:
                    # It's a file. Start ingestion.
                    # We use start_child_workflow to not block the BFS.
                    await workflow.start_child_workflow(
                        RepoFileIngestionWorkflow.run,
                        args=[owner, path],
                        id=f"repo-file-{owner}-{path.replace('/', '-')}",
                        task_queue=TEMPORAL_TASK_QUEUE,
                        parent_close_policy=workflow.ParentClosePolicy.ABANDON
                    )
                    files_processed += 1
                    
        workflow.logger.info(f"Started ingestion for {files_processed} files")
        return {"files_processed": files_processed}
