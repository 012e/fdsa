import logging
import requests
from temporalio import activity

logger = logging.getLogger(__name__)
GRAPHQL_URL = "http://localhost:8080/graphql"

@activity.defn
async def discover_files(owner: str) -> list[str]:
    """
    Discover all files in a repository using BFS via GraphQL.
    Returns a list of file paths.
    """
    logger.info(f"Discovering files for owner: {owner}")
    
    files = []
    queue = [""]  # Start with root
    
    # Prevent infinite loops
    visited = set()

    while queue:
        current_path = queue.pop(0)
        if current_path in visited:
            continue
        visited.add(current_path)
        
        logger.info(f"Listing files in: {current_path}")
        
        query = """
        query ListFiles($owner: String!, $path: String!) {
            listFilesByPath(owner: $owner, path: $path) {
                path
                isDirectory
            }
        }
        """
        
        try:
            response = requests.post(
                GRAPHQL_URL,
                json={
                    "query": query,
                    "variables": {"owner": owner, "path": current_path}
                },
                timeout=10
            )
            response.raise_for_status()
            result = response.json()
            
            if "errors" in result:
                logger.error(f"GraphQL errors: {result['errors']}")
                continue
                
            items = result.get("data", {}).get("listFilesByPath", [])
            
            for item in items:
                item_path = item["path"]
                if item["isDirectory"]:
                    if item_path not in visited:
                        queue.append(item_path)
                else:
                    files.append(item_path)
                    
        except Exception as e:
            logger.error(f"Error listing files for {current_path}: {e}")
            # Continue to next path in queue? Or fail?
            # For now, log and continue
            continue
            
    logger.info(f"Discovered {len(files)} files for owner {owner}")
    return files

@activity.defn
async def fetch_file_content(owner: str, path: str) -> str:
    """
    Fetch the content of a file via GraphQL.
    """
    logger.info(f"Fetching content for: {path}")
    
    query = """
    query GetSnippet($owner: String!, $path: String!) {
        snippetByPath(owner: $owner, path: $path) {
            code
        }
    }
    """
    
    try:
        response = requests.post(
            GRAPHQL_URL,
            json={
                "query": query,
                "variables": {"owner": owner, "path": path}
            },
            timeout=10
        )
        response.raise_for_status()
        result = response.json()
        
        if "errors" in result:
            raise Exception(f"GraphQL errors: {result['errors']}")
            
        snippet = result.get("data", {}).get("snippetByPath")
        if not snippet:
            raise Exception(f"No snippet found for path {path}")
            
        return snippet["code"]
        
    except Exception as e:
        logger.error(f"Error fetching content for {path}: {e}")
        raise

@activity.defn
async def list_directory(owner: str, path: str) -> list[dict]:
    """
    List files and directories in a specific path via GraphQL.
    Returns a list of dicts with 'path' and 'isDirectory'.
    """
    logger.info(f"Listing directory: {path} for owner: {owner}")
    
    query = """
    query ListFiles($owner: String!, $path: String!) {
        listFilesByPath(owner: $owner, path: $path) {
            path
            isDirectory
        }
    }
    """
    
    try:
        response = requests.post(
            GRAPHQL_URL,
            json={
                "query": query,
                "variables": {"owner": owner, "path": path}
            },
            timeout=10
        )
        response.raise_for_status()
        result = response.json()
        
        if "errors" in result:
            raise Exception(f"GraphQL errors: {result['errors']}")
            
        items = result.get("data", {}).get("listFilesByPath", [])
        return items
        
    except Exception as e:
        logger.error(f"Error listing directory {path}: {e}")
        raise
