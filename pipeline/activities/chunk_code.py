from temporalio import activity
from semchunk.semchunk import chunkerify
import tiktoken


@activity.defn
async def chunk_code(code: str) -> list[str]:
    """
    Activity to apply semantic chunking to code.
    
    Args:
        code: The source code to chunk
        
    Returns:
        A list of code chunks
    """
    # Use semchunk with a reasonable chunk size for code
    # Adjust chunk_size based on your embedding model's token limits
    # Using tiktoken's cl100k_base tokenizer (used by OpenAI embeddings)
    tokenizer = tiktoken.get_encoding("cl100k_base")
    chunker = chunkerify(tokenizer, chunk_size=512)
    result = chunker(code)
    
    # Handle different return types from semchunk
    raw_chunks = result[0] if isinstance(result, tuple) else result
    
    # Ensure we have a flat list of strings
    final_chunks: list[str] = []
    for item in raw_chunks:
        if isinstance(item, list):
            final_chunks.extend(item)
        else:
            final_chunks.append(str(item))
    
    return final_chunks
