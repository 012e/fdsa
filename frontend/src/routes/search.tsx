import { useEffect, useState } from 'react'
import { createFileRoute, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { codeSearchApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { CodeViewer } from '@/components/ui/code-viewer'
import { Search, FileCode, Hash, Filter, ChevronLeft, ChevronRight } from 'lucide-react'
import type { CodeSearchResponse, CodeSearchResult } from '@/lib/generated'
import z from 'zod'
import { zodValidator,} from '@tanstack/zod-adapter'

const searchSchema = z.object({
  q: z.string().catch('').default(''),
  repositoryIdentifier: z.string().optional(),
  language: z.string().optional(),
  fileExtension: z.string().optional(),
  filePathPattern: z.string().optional(),
  page: z.number().min(0).catch(0).default(0),
})

export const Route = createFileRoute('/search')({
  component: SearchPage,
  validateSearch: zodValidator(searchSchema),
})

type SearchParams = z.infer<typeof searchSchema>

function SearchPage() {
  // 3. Get the current search state from the URL
  const search = Route.useSearch()
  const navigate = useNavigate({ from: Route.fullPath })
  
  // Keep local state ONLY for the input field to prevent "laggy" typing
  const [localInput, setLocalInput] = useState(search.q)
  const [showFilters, setShowFilters] = useState(false)
  const size = 10

  // Sync local input if URL changes (e.g., user hits 'back' or clicks a link)
  useEffect(() => {
    setLocalInput(search.q)
  }, [search.q])

  const { data: searchResults, isLoading, error, isFetching } = useQuery({
    // 4. QueryKey depends on the entire search object from the URL
    queryKey: ['codeSearch', search],
    queryFn: async () => {
      if (!search.q) return null
      const response = await codeSearchApi.searchCode(
        search.q,
        search.repositoryIdentifier,
        search.language,
        search.filePathPattern,
        search.page,
        size,
      )
      return response.data as CodeSearchResponse
    },
    enabled: search.q.length > 0,
  })

  // 5. Update functions now update the URL, not local state
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    navigate({
      search: (prev) => ({ ...prev, q: localInput, page: 0 }),
      replace: true
    })
  }

  const updateFilter = (updates: Partial<SearchParams>) => {
    navigate({
      search: (prev) => ({ ...prev, ...updates, page: 0 }),
      replace: true
    })
  }

  const handleClearFilters = () => {
    navigate({
      search: (prev) => ({ q: prev.q, page: 0 }),
      replace: true
    })
  }

  const totalPages = searchResults?.totalPages || 0

  return (
    <div className="container mx-auto p-6 max-w-7xl">
      <div className="mb-8">
        <h1 className="text-4xl font-bold mb-2">Code Search</h1>
        <p className="text-muted-foreground">
          Search across all indexed code files with semantic understanding
        </p>
      </div>

      <Card className="mb-6">
        <CardContent className="pt-6">
          <form onSubmit={handleSearch} className="space-y-4">
            <div className="flex gap-2">
              <div className="flex-1">
                <Input
                  type="text"
                  placeholder="Search for code, functions, classes, variables..."
                  value={localInput}
                  onChange={(e) => setLocalInput(e.target.value)}
                  className="text-lg"
                />
              </div>
              <Button type="submit" size="lg" disabled={!localInput || isFetching}>
                <Search className="mr-2 h-4 w-4" />
                Search
              </Button>
              <Button
                type="button"
                variant="outline"
                size="lg"
                onClick={() => setShowFilters(!showFilters)}
              >
                <Filter className="mr-2 h-4 w-4" />
                Filters
              </Button>
            </div>

            {showFilters && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 pt-4 border-t">
                <div className="space-y-2">
                  <Label>Repository</Label>
                  <Input
                    placeholder="owner/repo"
                    value={search.repositoryIdentifier || ''}
                    onChange={(e) => updateFilter({ repositoryIdentifier: e.target.value || undefined })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Language</Label>
                  <Input
                    placeholder="e.g., Python, Java"
                    value={search.language || ''}
                    onChange={(e) => updateFilter({ language: e.target.value || undefined })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>File Extension</Label>
                  <Input
                    placeholder="e.g., .py, .js"
                    value={search.fileExtension || ''}
                    onChange={(e) => updateFilter({ fileExtension: e.target.value || undefined })}
                  />
                </div>
                <div className="space-y-2">
                  <Label>Path Pattern</Label>
                  <Input
                    placeholder="e.g., src/**/*.ts"
                    value={search.filePathPattern || ''}
                    onChange={(e) => updateFilter({ filePathPattern: e.target.value || undefined })}
                  />
                </div>
                <div className="col-span-full">
                  <Button type="button" variant="ghost" size="sm" onClick={handleClearFilters}>
                    Clear all filters
                  </Button>
                </div>
              </div>
            )}
          </form>
        </CardContent>
      </Card>

      {/* Results Section */}
      {searchResults && (
        <>
          <div className="space-y-4 mb-6">
            {searchResults.results!.map((result: CodeSearchResult) => (
              <SearchResultCard key={result.id} result={result} />
            ))}
          </div>

          {/* Updated Pagination to use URL state */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                Page {search.page + 1} of {totalPages}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigate({ search: (p) => ({ ...p, page: Math.max(0, p.page - 1) }) })}
                  disabled={search.page === 0 || isFetching}
                >
                  <ChevronLeft className="h-4 w-4 mr-1" /> Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => navigate({ search: (p) => ({ ...p, page: p.page + 1 }) })}
                  disabled={search.page >= totalPages - 1 || isFetching}
                >
                  Next <ChevronRight className="h-4 w-4 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  )
}
interface SearchResultCardProps {
  result: CodeSearchResult
}

function SearchResultCard({ result }: SearchResultCardProps) {
  const [expanded, setExpanded] = useState(false)
  const navigate = useNavigate()

  const handleCardClick = () => {
    if (!result.repositoryIdentifier || !result.filePath) return
    
    // Parse owner/repo from repositoryIdentifier
    const [owner, repo] = result.repositoryIdentifier.split('/')
    
    navigate({
      to: '/repositories/$owner/$repo',
      params: { owner, repo },
      search: { file: result.filePath },
    })
  }

  return (
    <Card 
      className="hover:shadow-md transition-shadow cursor-pointer"
      onClick={handleCardClick}
    >
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <CardTitle className="text-lg font-mono mb-2">
              <FileCode className="inline h-4 w-4 mr-2" />
              {result.fileName}
            </CardTitle>
            <CardDescription className="font-mono text-xs">
              {result.repositoryIdentifier && (
                <span className="mr-3">📁 {result.repositoryIdentifier}</span>
              )}
              {result.filePath}
            </CardDescription>
          </div>
          <div className="flex items-center gap-2 ml-4">
            {result.language && (
              <Badge variant="outline">{result.language}</Badge>
            )}
            {result.score && (
              <Badge variant="secondary">
                <Hash className="h-3 w-3 mr-1" />
                {result.score.toFixed(2)}
              </Badge>
            )}
          </div>
        </div>
      </CardHeader>
      <CardContent>
        {/* Highlights */}
        {result.highlights && Object.keys(result.highlights).length > 0 && (
          <div className="mb-4">
            {Object.entries(result.highlights).map(([field, highlights]) => (
              <div key={field} className="mb-2">
                <p className="text-xs font-semibold text-muted-foreground mb-1 uppercase">
                  {field}:
                </p>
                <div className="space-y-1">
                  {highlights.slice(0, expanded ? undefined : 3).map((highlight, idx) => (
                    <pre
                      key={idx}
                      className="text-sm bg-muted p-2 rounded overflow-x-auto"
                      dangerouslySetInnerHTML={{ __html: highlight }}
                    />
                  ))}
                </div>
                {highlights.length > 3 && !expanded && (
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={(e) => {
                      e.stopPropagation()
                      setExpanded(true)
                    }}
                    className="mt-2"
                  >
                    Show {highlights.length - 3} more highlights...
                  </Button>
                )}
              </div>
            ))}
          </div>
        )}

        {/* Matched Chunks */}
        {result.matchedChunks && result.matchedChunks.length > 0 && (
          <div className="mt-4 pt-4 border-t">
            <p className="text-xs font-semibold text-muted-foreground mb-2 uppercase">
              Matched Code Chunks ({result.matchedChunks.length}):
            </p>
            <div className="space-y-3">
              {result.matchedChunks.slice(0, expanded ? undefined : 2).map((chunk, idx) => (
                <div key={idx} className="border rounded overflow-hidden">
                  {chunk.startLine !== undefined && (
                    <div className="bg-muted px-3 py-1.5 border-b">
                      <p className="text-xs text-muted-foreground">
                        Lines {chunk.startLine}-{chunk.endLine}
                      </p>
                    </div>
                  )}
                  <div className="overflow-hidden">
                    <CodeViewer
                      code={chunk.content || ''}
                      fileName={result.fileName}
                      language={result.language}
                      height="200px"
                      showLineNumbers={true}
                    />
                  </div>
                  {chunk.highlights && chunk.highlights.length > 0 && (
                    <div className="bg-muted/50 px-3 py-2 border-t space-y-1">
                      {chunk.highlights.map((highlight, hIdx) => (
                        <p
                          key={hIdx}
                          className="text-xs text-muted-foreground italic"
                          dangerouslySetInnerHTML={{ __html: highlight }}
                        />
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
            {result.matchedChunks.length > 2 && !expanded && (
              <Button
                variant="ghost"
                size="sm"
                onClick={(e) => {
                  e.stopPropagation()
                  setExpanded(true)
                }}
                className="mt-2"
              >
                Show {result.matchedChunks.length - 2} more chunks...
              </Button>
            )}
          </div>
        )}

        {expanded && (
          <Button
            variant="ghost"
            size="sm"
            onClick={(e) => {
              e.stopPropagation()
              setExpanded(false)
            }}
            className="mt-2"
          >
            Show less
          </Button>
        )}

        {/* File Metadata */}
        <div className="mt-4 pt-4 border-t flex items-center gap-4 text-xs text-muted-foreground">
          {result.size !== undefined && (
            <span>{(result.size / 1024).toFixed(2)} KB</span>
          )}
          {result.createdAt && (
            <span>Indexed: {new Date(result.createdAt).toLocaleDateString()}</span>
          )}
        </div>
      </CardContent>
    </Card>
  )
}