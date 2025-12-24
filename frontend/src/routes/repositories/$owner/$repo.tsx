import { createFileRoute, Link, useNavigate } from '@tanstack/react-router'
import { useQuery } from '@tanstack/react-query'
import { repositoryApi } from '@/lib/api'
import { Card, CardContent, CardHeader } from '@/components/ui/card'
import { ArrowLeft } from 'lucide-react'
import { RepositoryHeader } from './-repository-header'
import { FileNavigator } from './-file-navigator'

export const Route = createFileRoute('/repositories/$owner/$repo')({
  component: RepositoryDetailPage,
  validateSearch: (search: Record<string, unknown>) => {
    return {
      path: (search.path as string) || '',
    }
  },
})

function RepositoryDetailPage() {
  const { owner, repo } = Route.useParams()
  const navigate = useNavigate()
  const { path: currentPath } = Route.useSearch()

  // Query repository using TanStack Query with repositoryApi
  const identifier = `${owner}/${repo}`
  const { data: repository, isLoading, error } = useQuery({
    queryKey: ['repository', identifier],
    queryFn: async () => {
      const response = await repositoryApi.getRepository(owner, repo)
      return response.data
    },
  })

  const handlePathChange = (newPath: string) => {
    navigate({
      to: '/repositories/$owner/$repo',
      params: { owner, repo },
      search: { path: newPath },
    })
  }

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto px-4 py-8 max-w-7xl">
          <Card className="animate-pulse">
            <CardHeader>
              <div className="h-8 bg-muted rounded w-1/3 mb-2"></div>
              <div className="h-4 bg-muted rounded w-2/3"></div>
            </CardHeader>
          </Card>
        </div>
      </div>
    )
  }

  if (error || !repository) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container mx-auto px-4 py-8 max-w-7xl">
          <Card className="border-destructive">
            <CardContent className="pt-6">
              <p className="text-destructive">
                Error loading repository: {String(error) || 'Repository not found'}
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container mx-auto px-4 py-8 max-w-7xl">
        {/* Back Button */}
        <Link to="/repositories" className="inline-flex items-center gap-2 text-muted-foreground hover:text-foreground mb-6">
          <ArrowLeft className="w-4 h-4" />
          Back to repositories
        </Link>

        {/* Repository Header */}
        <RepositoryHeader repository={repository} />

        {/* File Navigator */}
        <FileNavigator 
          owner={owner} 
          repo={repo} 
          currentPath={currentPath} 
          onPathChange={handlePathChange}
        />
      </div>
    </div>
  )
}
