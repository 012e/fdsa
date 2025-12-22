import { openai } from '@ai-sdk/openai';
import { embed, embedMany } from 'ai';
import { getConfig } from '@fdsa/shared';

/**
 * Activity to generate embeddings for a single text using OpenAI embeddings.
 * 
 * @param text - The text to embed
 * @returns A list of floats representing the embedding vector
 */
export async function embedText(text: string): Promise<number[]> {
  const config = getConfig();
  
  const result = await embed({
    model: openai.embedding(config.openai.embeddingModel, {
      dimensions: config.openai.embeddingDimensions,
    }),
    value: text,
  });

  return result.embedding;
}

/**
 * Activity to generate embeddings for multiple texts in batch.
 * 
 * @param texts - List of texts to embed
 * @returns List of embedding vectors
 */
export async function embedTexts(texts: string[]): Promise<number[][]> {
  const config = getConfig();
  
  const result = await embedMany({
    model: openai.embedding(config.openai.embeddingModel, {
      dimensions: config.openai.embeddingDimensions,
    }),
    values: texts,
  });

  return result.embeddings;
}
