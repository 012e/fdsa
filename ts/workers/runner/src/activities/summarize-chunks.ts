import { openai } from '@ai-sdk/openai';
import { generateText } from 'ai';
import { getConfig } from '@fdsa/shared';

/**
 * Activity to generate summaries for each code chunk.
 * 
 * @param chunks - List of code chunks
 * @returns List of summaries, one for each chunk
 */
export async function summarizeChunks(chunks: string[]): Promise<string[]> {
  const config = getConfig();
  const summaries: string[] = [];

  for (let i = 0; i < chunks.length; i++) {
    const chunkText = chunks[i];
    
    const result = await generateText({
      model: openai(config.openai.model),
      prompt: `You are a code summarization assistant. Analyze the provided code snippet and create a 
brief, informative summary. Focus on what this specific portion of code does and its purpose.
Keep it concise but meaningful.

Please summarize this code chunk (part ${i + 1}):

${chunkText}`,
    });

    summaries.push(result.text);
  }

  return summaries;
}
