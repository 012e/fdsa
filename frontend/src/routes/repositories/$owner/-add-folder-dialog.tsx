import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { repositoryApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { FolderPlus } from 'lucide-react'

interface AddFolderDialogProps {
  owner: string
  repo: string
  identifier: string
  currentPath: string
}

export function AddFolderDialog({ owner, repo, identifier, currentPath }: AddFolderDialogProps) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [folderName, setFolderName] = useState('')
  const [commitMessage, setCommitMessage] = useState('')

  const createFolderMutation = useMutation({
    mutationFn: async (data: { path: string; commitMessage: string }) => {
      const response = await repositoryApi.createRepositoryFolder(owner, repo, {
        path: data.path,
        commitMessage: data.commitMessage,
      })
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repository', identifier] })
      queryClient.invalidateQueries({ queryKey: ['directory'] })
      setFolderName('')
      setCommitMessage('')
      setOpen(false)
    },
    onError: (error: any) => {
      toast.error('Failed to create folder', {
        description: error?.response?.data?.message || error?.message || 'An error occurred while creating the folder',
      })
    },
  })

  const handleCreateFolder = () => {
    if (!folderName.trim() || !commitMessage.trim()) return
    const fullPath = currentPath ? `${currentPath}/${folderName}` : folderName
    createFolderMutation.mutate({
      path: fullPath,
      commitMessage: commitMessage,
    })
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button variant="outline" size="sm" className="gap-2">
          <FolderPlus className="w-4 h-4" />
          New Folder
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-125">
        <DialogHeader>
          <DialogTitle>Create New Folder</DialogTitle>
          <DialogDescription>
            Create a new folder in the repository
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid gap-2">
            <Label htmlFor="folder-path">Folder Name</Label>
            <Input
              id="folder-path"
              placeholder="components"
              value={folderName}
              onChange={(e) => setFolderName(e.target.value)}
            />
            {currentPath && (
              <p className="text-sm text-muted-foreground">
                Will be created at: {currentPath}/{folderName || '...'}
              </p>
            )}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="folder-commit">Commit Message</Label>
            <Input
              id="folder-commit"
              placeholder="Create new folder"
              value={commitMessage}
              onChange={(e) => setCommitMessage(e.target.value)}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)}>
            Cancel
          </Button>
          <Button
            onClick={handleCreateFolder}
            disabled={!folderName.trim() || !commitMessage.trim() || createFolderMutation.isPending}
          >
            {createFolderMutation.isPending ? 'Creating...' : 'Create Folder'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
