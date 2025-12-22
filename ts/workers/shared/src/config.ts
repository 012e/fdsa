import { z } from 'zod';
import dotenv from 'dotenv';
import path from 'path';
import { fileURLToPath } from 'url';

// Load .env from ts/ root directory
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const envPath = path.resolve(__dirname, '../../../.env');
dotenv.config({ path: envPath });

/**
 * Configuration schema with validation
 */
const ConfigSchema = z.object({
  // OpenAI Configuration
  openai: z.object({
    apiKey: z.string().min(1, 'OPENAI_API_KEY is required'),
    model: z.string().default('gpt-4o-mini'),
    embeddingModel: z.string().default('text-embedding-3-large'),
    embeddingDimensions: z.number().default(1024),
  }),

  // Kafka Configuration
  kafka: z.object({
    bootstrapServers: z.string().default('localhost:9092'),
    groupId: z.string().default('snippet-ingestion-worker'),
    topics: z.object({
      snippetCreated: z.string().default('snippet.created'),
      snippetUpdated: z.string().default('snippet.updated'),
      snippetDeleted: z.string().default('snippet.deleted'),
    }),
  }),

  // Temporal Configuration
  temporal: z.object({
    host: z.string().default('localhost:7233'),
    namespace: z.string().default('default'),
    taskQueue: z.string().default('snippet-ingestion-queue'),
  }),

  // OpenSearch Configuration
  opensearch: z.object({
    host: z.string().default('localhost'),
    port: z.number().default(9200),
    index: z.string().default('code_snippets'),
  }),
});

export type Config = z.infer<typeof ConfigSchema>;

/**
 * Parse and validate environment variables
 */
export function loadConfig(): Config {
  try {
    return ConfigSchema.parse({
      openai: {
        apiKey: process.env.OPENAI_API_KEY,
        model: process.env.OPENAI_MODEL,
        embeddingModel: process.env.OPENAI_EMBEDDING_MODEL,
        embeddingDimensions: process.env.OPENAI_EMBEDDING_DIMENSIONS
          ? parseInt(process.env.OPENAI_EMBEDDING_DIMENSIONS, 10)
          : undefined,
      },
      kafka: {
        bootstrapServers: process.env.KAFKA_BOOTSTRAP_SERVERS,
        groupId: process.env.KAFKA_GROUP_ID,
        topics: {
          snippetCreated: process.env.KAFKA_TOPIC_SNIPPET_CREATED,
          snippetUpdated: process.env.KAFKA_TOPIC_SNIPPET_UPDATED,
          snippetDeleted: process.env.KAFKA_TOPIC_SNIPPET_DELETED,
        },
      },
      temporal: {
        host: process.env.TEMPORAL_HOST,
        namespace: process.env.TEMPORAL_NAMESPACE,
        taskQueue: process.env.TEMPORAL_TASK_QUEUE,
      },
      opensearch: {
        host: process.env.OPENSEARCH_HOST,
        port: process.env.OPENSEARCH_PORT
          ? parseInt(process.env.OPENSEARCH_PORT, 10)
          : undefined,
        index: process.env.OPENSEARCH_INDEX,
      },
    });
  } catch (error) {
    if (error instanceof z.ZodError) {
      console.error('Configuration validation failed:');
      error.errors.forEach((err) => {
        console.error(`  - ${err.path.join('.')}: ${err.message}`);
      });
    }
    throw new Error('Failed to load configuration');
  }
}

/**
 * Singleton config instance
 */
let config: Config | null = null;

export function getConfig(): Config {
  if (!config) {
    config = loadConfig();
  }
  return config;
}
