import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { repositoryApi } from '@/lib/api'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Button } from '@/components/ui/button'
import { File, Folder, ChevronRight, Home, Trash2, MoreVertical } from 'lucide-react'
import { DirectoryContentEntriesInner, EntryTypeEnum } from '@/lib/generated'
import { useState, useEffect } from 'react'
import { EditFileDialog } from './-edit-file-dialog'
import { AddFileDialog } from './-add-file-dialog'
import { AddFolderDialog } from './-add-folder-dialog'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { toast } from 'sonner'

interface FileNavigatorProps {
  owner: string
  repo: string
  currentPath: string
  onPathChange: (path: string) => void
  fileToOpen?: string
}

export function FileNavigator({ owner, repo, currentPath, onPathChange, fileToOpen }: FileNavigatorProps) {
  const identifier = `${owner}/${repo}`
  const queryClient = useQueryClient()
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [editFilePath, setEditFilePath] = useState('')
  const [editFileContent, setEditFileContent] = useState('')
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [deleteTarget, setDeleteTarget] = useState<{ path: string; type: EntryTypeEnum } | null>(null)
  const [deleteCommitMessage, setDeleteCommitMessage] = useState('')

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

  // Handle opening file from search results
  useEffect(() => {
    if (fileToOpen) {
      const openFile = async () => {
        setEditFilePath(fileToOpen)
        const response = await repositoryApi.readRepositoryFile(owner, repo, fileToOpen)
        if (response.data) {
          setEditFileContent(response.data.content || '')
          setEditDialogOpen(true)
        }
      }
      openFile().catch(error => {
        toast.error('Failed to open file', {
          description: error?.response?.data?.message || error?.message || 'An error occurred',
        })
      })
    }
  }, [fileToOpen, owner, repo])

  const deleteMutation = useMutation({
    mutationFn: async (data: { path: string; type: EntryTypeEnum; commitMessage: string }) => {
      if (data.type === EntryTypeEnum.File) {
        return await repositoryApi.deleteRepositoryFile(owner, repo, {
          path: data.path,
          commitMessage: data.commitMessage,
        })
      } else {
        return await repositoryApi.deleteRepositoryFolder(owner, repo, {
          path: data.path,
          commitMessage: data.commitMessage,
        })
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repository', identifier] })
      queryClient.invalidateQueries({ queryKey: ['directory'] })
      toast.success('Deleted successfully')
      setDeleteDialogOpen(false)
      setDeleteTarget(null)
      setDeleteCommitMessage('')
    },
    onError: (error: any) => {
      toast.error('Failed to delete', {
        description: error?.response?.data?.message || error?.message || 'An error occurred',
      })
    },
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

  const handleDeleteClick = (entry: DirectoryContentEntriesInner, e: React.MouseEvent) => {
    e.stopPropagation()
    setDeleteTarget({ path: entry.path || '', type: entry.type || EntryTypeEnum.File })
    setDeleteCommitMessage(`Delete ${entry.name}`)
    setDeleteDialogOpen(true)
  }

  const handleConfirmDelete = () => {
    if (!deleteTarget || !deleteCommitMessage.trim()) return
    deleteMutation.mutate({
      path: deleteTarget.path,
      type: deleteTarget.type,
      commitMessage: deleteCommitMessage,
    })
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
                <div key={entry.path} className="flex items-center gap-1 w-full">
                  <Button
                    variant="ghost"
                    className="flex-1 justify-start gap-3 h-auto py-2 hover:bg-accent"
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
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="h-8 w-8 p-0"
                        onClick={(e) => e.stopPropagation()}
                      >
                        <MoreVertical className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      <DropdownMenuItem
                        className="text-destructive focus:text-destructive"
                        onClick={(e) => handleDeleteClick(entry, e)}
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        Delete {entry.type === EntryTypeEnum.Directory ? 'folder' : 'file'}
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </div>
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

      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Confirm Deletion</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete{' '}
              <span className="font-semibold">{deleteTarget?.path}</span>?
              {deleteTarget?.type === EntryTypeEnum.Directory && (
                <span className="block mt-2 text-yellow-600">
                  Warning: This will delete the entire folder and all its contents.
                </span>
              )}
            </DialogDescription>
          </DialogHeader>
          <div className="space-y-4 py-4">
            <div className="space-y-2">
              <Label htmlFor="commit-message">Commit Message</Label>
              <Input
                id="commit-message"
                value={deleteCommitMessage}
                onChange={(e) => setDeleteCommitMessage(e.target.value)}
                placeholder="Enter commit message..."
              />
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)}>
              Cancel
            </Button>
            <Button
              variant="destructive"
              onClick={handleConfirmDelete}
              disabled={!deleteCommitMessage.trim() || deleteMutation.isPending}
            >
              {deleteMutation.isPending ? 'Deleting...' : 'Delete'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

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
