from temporalio import activity
from ml.embeder import embeder


@activity.defn
async def embed_text(text: str) -> list[float]:
    """
    Activity to generate embeddings for text using OpenAI embeddings.
    
    Args:
        text: The text to embed
        
    Returns:
        A list of floats representing the embedding vector
    """
    embedding = embeder.embed_query(text)
    return embedding


@activity.defn
async def embed_texts(texts: list[str]) -> list[list[float]]:
    """
    Activity to generate embeddings for multiple texts in batch.
    
    Args:
        texts: List of texts to embed
        
    Returns:
        List of embedding vectors
    """
    embeddings = embeder.embed_documents(texts)
    return embeddings
