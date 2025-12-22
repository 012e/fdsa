import { openai } from '@ai-sdk/openai';
import { generateText } from 'ai';
import { getConfig } from '@fdsa/shared';

/**
 * Activity to generate an overall summary for a piece of code using OpenAI.
 * 
 * @param code - The source code to summarize
 * @returns A summary of the code
 */
export async function summarizeCode(code: string): Promise<string> {
  const config = getConfig();
  
  const result = await generateText({
    model: openai(config.openai.model),
    prompt: `You are a code summarization assistant. Analyze the provided code and create a concise, 
informative summary that describes what the code does, its main purpose, key components, 
and functionality. Focus on the high-level behavior and intent rather than implementation details.
Keep the summary clear and professional.

Please summarize this code:

${code}`,
  });

  return result.text;
}
