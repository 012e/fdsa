from temporalio import activity
from ml.llm import llm


@activity.defn
async def summarize_chunks(chunks: list[str]) -> list[str]:
    """
    Activity to generate summaries for each code chunk.
    
    Args:
        chunks: List of code chunks
        
    Returns:
        List of summaries, one for each chunk
    """
    summaries = []
    
    for i, chunk_text in enumerate(chunks):
        result = llm.invoke(
            [
                (
                    "system",
                    """You are a code summarization assistant. Analyze the provided code snippet and create a 
                    brief, informative summary. Focus on what this specific portion of code does and its purpose.
                    Keep it concise but meaningful.""",
                ),
                ("user", f"Please summarize this code chunk (part {i+1}):\n\n{chunk_text}"),
            ],
        )
        content = result.content
        summaries.append(content if isinstance(content, str) else str(content))
    
    return summaries
