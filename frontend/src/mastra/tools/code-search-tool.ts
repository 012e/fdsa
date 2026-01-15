import { createTool } from "@mastra/core/tools";
import { z } from "zod";
import { codeSearchApi } from "@/lib/api";

const codeSearchResultSchema = z.object({
  id: z.string(),
  fileName: z.string(),
  filePath: z.string(),
  content: z.string().optional(),
  language: z.string().optional(),
  repositoryIdentifier: z.string().optional(),
  size: z.number().optional(),
  score: z.number().optional(),
  createdAt: z.string().optional(),
  highlights: z.record(z.string(), z.array(z.string())).optional(),
});

const codeSearchResponseSchema = z.object({
  results: z.array(codeSearchResultSchema).optional(),
  totalElements: z.number().optional(),
  totalPages: z.number().optional(),
  currentPage: z.number().optional(),
  pageSize: z.number().optional(),
});

export const codeSearchTool = createTool({
  id: "search-code",
  description: "Search for code snippets across indexed repositories using semantic search. Returns relevant code files, functions, classes, and variables.",
  inputSchema: z.object({
    query: z.string().describe("The search query - can be natural language or code-specific terms"),
    repositoryIdentifier: z.string().optional().describe("Filter by repository (format: owner/repo)"),
    language: z.string().optional().describe("Filter by programming language (e.g., Python, Java, TypeScript)"),
    filePathPattern: z.string().optional().describe("Filter by file path pattern (e.g., src/**/*.ts)"),
    page: z.number().optional().default(0).describe("Page number for pagination (0-indexed)"),
    size: z.number().optional().default(10).describe("Number of results per page"),
  }),
  outputSchema: codeSearchResponseSchema,
  execute: async (inputData) => {
    try {
      const response = await codeSearchApi.searchCode(
        inputData.query,
        inputData.repositoryIdentifier,
        inputData.language,
        inputData.filePathPattern,
        inputData.page,
        inputData.size,
      );
      
      return response.data;
    } catch (error) {
      console.error("Error searching code:", error);
      throw new Error(`Failed to search code: ${error instanceof Error ? error.message : "Unknown error"}`);
    }
  },
});
