from temporalio import activity
from ml.llm import llm


@activity.defn
async def summarize_code(code: str) -> str:
    """
    Activity to generate an overall summary for a piece of code using LangChain.
    
    Args:
        code: The source code to summarize
        
    Returns:
        A summary of the code
    """
    result = llm.invoke(
        [
            (
                "system",
                """You are a code summarization assistant. Analyze the provided code and create a concise, 
                informative summary that describes what the code does, its main purpose, key components, 
                and functionality. Focus on the high-level behavior and intent rather than implementation details.
                Keep the summary clear and professional.""",
            ),
            ("user", f"Please summarize this code:\n\n{code}"),
        ],
    )
    # Ensure we return a string
    content = result.content
    return content if isinstance(content, str) else str(content)
