import { z } from 'zod';

/**
 * Zod schema for SnippetCreatedEvent
 * Matches Java's SnippetCreatedEvent DTO
 */
export const SnippetCreatedEventSchema = z.object({
  id: z.string().uuid(),
  code: z.string(),
});

export type SnippetCreatedEvent = z.infer<typeof SnippetCreatedEventSchema>;

/**
 * Zod schema for SnippetUpdatedEvent
 * Matches Java's SnippetUpdatedEvent DTO
 */
export const SnippetUpdatedEventSchema = z.object({
  id: z.string().uuid(),
  code: z.string(),
});

export type SnippetUpdatedEvent = z.infer<typeof SnippetUpdatedEventSchema>;

/**
 * Zod schema for SnippetDeletedEvent
 * Matches Java's SnippetDeletedEvent DTO
 */
export const SnippetDeletedEventSchema = z.object({
  id: z.string().uuid(),
});

export type SnippetDeletedEvent = z.infer<typeof SnippetDeletedEventSchema>;
