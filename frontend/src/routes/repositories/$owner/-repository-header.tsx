import { Card, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { GitBranch, Folder } from 'lucide-react'
import { Repository } from '@/lib/generated'

interface RepositoryHeaderProps {
  repository: Repository
}

export function RepositoryHeader({ repository }: RepositoryHeaderProps) {
  const [owner, repo] = (repository.identifier || '/').split('/')

  return (
    <Card className="mb-6">
      <CardHeader>
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <CardTitle className="flex items-center gap-3 mb-3">
              <GitBranch className="w-6 h-6 text-muted-foreground" />
              <span className="text-3xl">{owner}/{repo}</span>
              <Badge variant="secondary">Repository</Badge>
            </CardTitle>
            <CardDescription className="text-base mb-4">
              {repository.description || 'No description provided'}
            </CardDescription>
            <div className="flex items-center gap-2 text-sm">
              <Folder className="w-4 h-4 text-muted-foreground" />
              <code className="text-xs bg-muted px-3 py-1.5 rounded">
                {repository.filesystemPath}
              </code>
            </div>
          </div>
        </div>
      </CardHeader>
    </Card>
  )
}
