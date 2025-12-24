import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { repositoryApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Plus } from 'lucide-react'

interface AddFileDialogProps {
  owner: string
  repo: string
  identifier: string
  currentPath: string
}

export function AddFileDialog({ owner, repo, identifier, currentPath }: AddFileDialogProps) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState(false)
  const [fileName, setFileName] = useState('')
  const [fileContent, setFileContent] = useState('')
  const [commitMessage, setCommitMessage] = useState('')

  const addFileMutation = useMutation({
    mutationFn: async (data: { path: string; content: string; commitMessage: string }) => {
      const response = await repositoryApi.addRepositoryFile(owner, repo, {
        path: data.path,
        content: data.content,
        commitMessage: data.commitMessage,
      })
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repository', identifier] })
      queryClient.invalidateQueries({ queryKey: ['directory'] })
      setFileName('')
      setFileContent('')
      setCommitMessage('')
      setOpen(false)
    },
    onError: (error: any) => {
      toast.error('Failed to add file', {
        description: error?.response?.data?.message || error?.message || 'An error occurred while adding the file',
      })
    },
  })

  const handleAddFile = () => {
    if (!fileName.trim() || !commitMessage.trim()) return
    const fullPath = currentPath ? `${currentPath}/${fileName}` : fileName
    addFileMutation.mutate({
      path: fullPath,
      content: fileContent,
      commitMessage: commitMessage,
    })
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button size="sm" className="gap-2">
          <Plus className="w-4 h-4" />
          New File
        </Button>
      </DialogTrigger>
      <DialogContent className="sm:max-w-125">
        <DialogHeader>
          <DialogTitle>Add New File</DialogTitle>
          <DialogDescription>
            Create a new file in the repository
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4">
          <div className="grid gap-2">
            <Label htmlFor="file-path">File Name</Label>
            <Input
              id="file-path"
              placeholder="index.ts"
              value={fileName}
              onChange={(e) => setFileName(e.target.value)}
            />
            {currentPath && (
              <p className="text-sm text-muted-foreground">
                Will be created at: {currentPath}/{fileName || '...'}
              </p>
            )}
          </div>
          <div className="grid gap-2">
            <Label htmlFor="file-content">Content</Label>
            <Textarea
              id="file-content"
              placeholder="File content..."
              value={fileContent}
              onChange={(e) => setFileContent(e.target.value)}
              rows={8}
              className="font-mono text-sm"
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="file-commit">Commit Message</Label>
            <Input
              id="file-commit"
              placeholder="Add new file"
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
            onClick={handleAddFile}
            disabled={!fileName.trim() || !commitMessage.trim() || addFileMutation.isPending}
          >
            {addFileMutation.isPending ? 'Adding...' : 'Add File'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
