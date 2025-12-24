import { useQuery } from '@tanstack/react-query'
import { repositoryApi } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Button } from '@/components/ui/button'
import { File, Folder, ChevronRight, Home } from 'lucide-react'
import { DirectoryContentEntriesInner, EntryTypeEnum } from '@/lib/generated'
import { useState } from 'react'
import { EditFileDialog } from './-edit-file-dialog'
import { AddFileDialog } from './-add-file-dialog'
import { AddFolderDialog } from './-add-folder-dialog'

interface FileNavigatorProps {
  owner: string
  repo: string
  currentPath: string
  onPathChange: (path: string) => void
}

export function FileNavigator({ owner, repo, currentPath, onPathChange }: FileNavigatorProps) {
  const identifier = `${owner}/${repo}`
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [editFilePath, setEditFilePath] = useState('')
  const [editFileContent, setEditFileContent] = useState('')

  // Query directory contents
  const { data: directoryContent, isLoading: isLoadingDir } = useQuery({
    queryKey: ['directory', identifier, currentPath],
    queryFn: async () => {
      const response = await repositoryApi.listRepositoryDirectory(
        owner,
        repo,
        currentPath || undefined
      )
      return response.data
    },
  })

  // Query file content when a file is clicked
  const { refetch: refetchFile } = useQuery({
    queryKey: ['file', identifier, editFilePath],
    queryFn: async () => {
      if (!editFilePath) return null
      const response = await repositoryApi.readRepositoryFile(owner, repo, editFilePath)
      return response.data
    },
    enabled: false,
  })

  const handleEntryClick = async (entry: DirectoryContentEntriesInner) => {
    if (entry.type === EntryTypeEnum.Directory) {
      onPathChange(entry.path || '')
    } else if (entry.type === EntryTypeEnum.File) {
      setEditFilePath(entry.path || '')
      const result = await refetchFile()
      if (result.data) {
        setEditFileContent(result.data.content || '')
        setEditDialogOpen(true)
      }
    }
  }

  const handleNavigateUp = () => {
    const parts = currentPath.split('/').filter(Boolean)
    parts.pop()
    onPathChange(parts.join('/'))
  }

  const handleBreadcrumbClick = (index: number) => {
    if (index === -1) {
      onPathChange('')
      return
    }
    const parts = currentPath.split('/').filter(Boolean)
    onPathChange(parts.slice(0, index + 1).join('/'))
  }

  const pathParts = currentPath ? currentPath.split('/').filter(Boolean) : []

  return (
    <>
      <Card>
        <CardHeader>
          <div className="flex items-center justify-between">
            <div>
              <CardTitle className="text-xl">Repository Files</CardTitle>
              <CardDescription>Browse and manage files in this repository</CardDescription>
            </div>
            <div className="flex gap-2">
              <AddFolderDialog owner={owner} repo={repo} identifier={identifier} currentPath={currentPath} />
              <AddFileDialog owner={owner} repo={repo} identifier={identifier} currentPath={currentPath} />
            </div>
          </div>
        </CardHeader>
        <Separator />
        <CardContent className="pt-6">
          {/* Breadcrumb navigation */}
          <div className="flex items-center gap-2 mb-4 text-sm">
            <Button
              variant="ghost"
              size="sm"
              className="gap-2"
              onClick={() => handleBreadcrumbClick(-1)}
            >
              <Home className="w-4 h-4" />
              {repo}
            </Button>
            {pathParts.map((part, index) => (
              <div key={index} className="flex items-center gap-2">
                <ChevronRight className="w-4 h-4 text-muted-foreground" />
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => handleBreadcrumbClick(index)}
                >
                  {part}
                </Button>
              </div>
            ))}
          </div>

          {/* File listing */}
          {isLoadingDir ? (
            <div className="text-sm text-muted-foreground">Loading...</div>
          ) : (
            <div className="space-y-1">
              {currentPath && (
                <Button
                  variant="ghost"
                  className="w-full justify-start gap-3 h-auto py-2"
                  onClick={handleNavigateUp}
                >
                  <Folder className="w-4 h-4 text-muted-foreground" />
                  <span className="font-medium">..</span>
                </Button>
              )}
              {directoryContent?.entries?.map((entry) => (
                <Button
                  key={entry.path}
                  variant="ghost"
                  className="w-full justify-start gap-3 h-auto py-2 hover:bg-accent"
                  onClick={() => handleEntryClick(entry)}
                >
                  {entry.type === EntryTypeEnum.Directory ? (
                    <Folder className="w-4 h-4 text-blue-500" />
                  ) : (
                    <File className="w-4 h-4 text-muted-foreground" />
                  )}
                  <span className="font-medium">{entry.name}</span>
                  {entry.type === EntryTypeEnum.File && 'size' in entry && (
                    <span className="ml-auto text-xs text-muted-foreground">
                      {formatBytes(entry.size || 0)}
                    </span>
                  )}
                </Button>
              ))}
              {(!directoryContent?.entries || directoryContent.entries.length === 0) && !currentPath && (
                <div className="text-sm text-muted-foreground py-4 text-center">
                  This directory is empty. Create a new file or folder to get started.
                </div>
              )}
            </div>
          )}
        </CardContent>
      </Card>

      <EditFileDialog
        owner={owner}
        repo={repo}
        identifier={identifier}
        open={editDialogOpen}
        onOpenChange={setEditDialogOpen}
        filePath={editFilePath}
        fileContent={editFileContent}
      />
    </>
  )
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 Bytes'
  const k = 1024
  const sizes = ['Bytes', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
}
