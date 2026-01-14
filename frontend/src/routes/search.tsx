import { useState } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { codeSearchApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Badge } from '@/components/ui/badge'
import { CodeViewer } from '@/components/ui/code-viewer'
import { Search, FileCode, Clock, Hash, Filter, ChevronLeft, ChevronRight } from 'lucide-react'
import type { CodeSearchResponse, CodeSearchResult } from '@/lib/generated'

export const Route = createFileRoute('/search')({
  component: SearchPage,
})

function SearchPage() {
  const [query, setQuery] = useState('')
  const [searchQuery, setSearchQuery] = useState('')
  const [repositoryIdentifier, setRepositoryIdentifier] = useState('')
  const [language, setLanguage] = useState('')
  const [fileExtension, setFileExtension] = useState('')
  const [filePathPattern, setFilePathPattern] = useState('')
  const [page, setPage] = useState(0)
  const size = 10
  const [showFilters, setShowFilters] = useState(false)

  // Query code search using TanStack Query
  const { data: searchResults, isLoading, error, isFetching } = useQuery({
    queryKey: ['codeSearch', searchQuery, repositoryIdentifier, language, fileExtension, filePathPattern, page, size],
    queryFn: async () => {
      if (!searchQuery) return null
      const response = await codeSearchApi.searchCode(
        searchQuery,
        undefined, // repositoryId
        repositoryIdentifier || undefined,
        language || undefined,
        fileExtension || undefined,
        filePathPattern || undefined,
        page,
        size,
        'content,fileName,filePath' // highlight fields
      )
      return response.data as CodeSearchResponse
    },
    enabled: searchQuery.length > 0,
  })

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setSearchQuery(query)
    setPage(0) // Reset to first page on new search
  }

  const handleClearFilters = () => {
    setRepositoryIdentifier('')
    setLanguage('')
    setFileExtension('')
    setFilePathPattern('')
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

      {/* Search Form */}
      <Card className="mb-6">
        <CardContent className="pt-6">
          <form onSubmit={handleSearch} className="space-y-4">
            <div className="flex gap-2">
              <div className="flex-1">
                <Input
                  type="text"
                  placeholder="Search for code, functions, classes, variables..."
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  className="text-lg"
                />
              </div>
              <Button type="submit" size="lg" disabled={!query || isFetching}>
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

            {/* Filters */}
            {showFilters && (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 pt-4 border-t">
                <div className="space-y-2">
                  <Label htmlFor="repository">Repository</Label>
                  <Input
                    id="repository"
                    type="text"
                    placeholder="owner/repo"
                    value={repositoryIdentifier}
                    onChange={(e) => setRepositoryIdentifier(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="language">Language</Label>
                  <Input
                    id="language"
                    type="text"
                    placeholder="e.g., Python, Java"
                    value={language}
                    onChange={(e) => setLanguage(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="extension">File Extension</Label>
                  <Input
                    id="extension"
                    type="text"
                    placeholder="e.g., .py, .js"
                    value={fileExtension}
                    onChange={(e) => setFileExtension(e.target.value)}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="pathPattern">Path Pattern</Label>
                  <Input
                    id="pathPattern"
                    type="text"
                    placeholder="e.g., src/**/*.ts"
                    value={filePathPattern}
                    onChange={(e) => setFilePathPattern(e.target.value)}
                  />
                </div>
                <div className="col-span-full">
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    onClick={handleClearFilters}
                  >
                    Clear all filters
                  </Button>
                </div>
              </div>
            )}
          </form>
        </CardContent>
      </Card>

      {/* Loading State */}
      {isLoading && (
        <div className="text-center py-12">
          <div className="inline-block animate-spin rounded-full h-8 w-8 border-b-2 border-gray-900"></div>
          <p className="mt-4 text-muted-foreground">Searching...</p>
        </div>
      )}

      {/* Error State */}
      {error && (
        <Card className="border-red-200 bg-red-50">
          <CardContent className="pt-6">
            <p className="text-red-600">
              Error searching code: {error.message}
            </p>
          </CardContent>
        </Card>
      )}

      {/* Results */}
      {searchResults && searchResults.results && (
        <>
          {/* Results Header */}
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-4">
              <p className="text-sm text-muted-foreground">
                Found <strong>{searchResults.totalHits}</strong> results
                {searchResults.tookMs && (
                  <span className="ml-2">
                    <Clock className="inline h-3 w-3 mr-1" />
                    {searchResults.tookMs}ms
                  </span>
                )}
              </p>
              {(repositoryIdentifier || language || fileExtension || filePathPattern) && (
                <div className="flex gap-2">
                  {repositoryIdentifier && (
                    <Badge variant="secondary">Repo: {repositoryIdentifier}</Badge>
                  )}
                  {language && (
                    <Badge variant="secondary">Lang: {language}</Badge>
                  )}
                  {fileExtension && (
                    <Badge variant="secondary">Ext: {fileExtension}</Badge>
                  )}
                  {filePathPattern && (
                    <Badge variant="secondary">Path: {filePathPattern}</Badge>
                  )}
                </div>
              )}
            </div>
          </div>

          {/* Results List */}
          <div className="space-y-4 mb-6">
            {searchResults.results.length === 0 ? (
              <Card>
                <CardContent className="pt-6 text-center py-12">
                  <FileCode className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
                  <p className="text-muted-foreground">No results found for your search.</p>
                  <p className="text-sm text-muted-foreground mt-2">
                    Try different keywords or adjust your filters.
                  </p>
                </CardContent>
              </Card>
            ) : (
              searchResults.results.map((result: CodeSearchResult) => (
                <SearchResultCard key={result.id} result={result} />
              ))
            )}
          </div>

          {/* Pagination */}
          {totalPages > 1 && (
            <div className="flex items-center justify-between">
              <p className="text-sm text-muted-foreground">
                Page {page + 1} of {totalPages}
              </p>
              <div className="flex gap-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.max(0, page - 1))}
                  disabled={page === 0 || isFetching}
                >
                  <ChevronLeft className="h-4 w-4 mr-1" />
                  Previous
                </Button>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
                  disabled={page >= totalPages - 1 || isFetching}
                >
                  Next
                  <ChevronRight className="h-4 w-4 ml-1" />
                </Button>
              </div>
            </div>
          )}
        </>
      )}

      {/* No Search Yet */}
      {!searchQuery && !isLoading && (
        <Card>
          <CardContent className="pt-6 text-center py-12">
            <Search className="mx-auto h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">
              Enter a search query to find code across all repositories.
            </p>
            <p className="text-sm text-muted-foreground mt-2">
              Search supports semantic understanding and highlights matching code.
            </p>
          </CardContent>
        </Card>
      )}
    </div>
  )
}

interface SearchResultCardProps {
  result: CodeSearchResult
}

function SearchResultCard({ result }: SearchResultCardProps) {
  const [expanded, setExpanded] = useState(false)

  return (
    <Card className="hover:shadow-md transition-shadow">
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
                    onClick={() => setExpanded(true)}
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
                onClick={() => setExpanded(true)}
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
            onClick={() => setExpanded(false)}
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

