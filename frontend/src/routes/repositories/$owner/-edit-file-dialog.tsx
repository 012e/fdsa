import { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { toast } from 'sonner'
import { repositoryApi } from '@/lib/api'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { CodeEditor, detectLanguageFromPath } from '@/components/ui/code-editor'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'

interface EditFileDialogProps {
  owner: string
  repo: string
  identifier: string
  open: boolean
  onOpenChange: (open: boolean) => void
  filePath: string
  fileContent: string
}

export function EditFileDialog({
  owner,
  repo,
  identifier,
  open,
  onOpenChange,
  filePath: initialFilePath,
  fileContent: initialFileContent,
}: EditFileDialogProps) {
  const queryClient = useQueryClient()
  const [filePath, setFilePath] = useState(initialFilePath)
  const [fileContent, setFileContent] = useState(initialFileContent)
  const [commitMessage, setCommitMessage] = useState('')

  useEffect(() => {
    setFilePath(initialFilePath)
    setFileContent(initialFileContent)
  }, [initialFilePath, initialFileContent])

  const updateFileMutation = useMutation({
    mutationFn: async (data: { path: string; content: string; commitMessage: string }) => {
      const response = await repositoryApi.updateRepositoryFile(owner, repo, {
        path: data.path,
        content: data.content,
        commitMessage: data.commitMessage,
      })
      return response.data
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['repository', identifier] })
      queryClient.invalidateQueries({ queryKey: ['directory'] })
      queryClient.invalidateQueries({ queryKey: ['file'] })
      setFilePath('')
      setFileContent('')
      setCommitMessage('')
      onOpenChange(false)
    },
    onError: (error: any) => {
      toast.error('Failed to update file', {
        description: error?.response?.data?.message || error?.message || 'An error occurred while updating the file',
      })
    },
  })

  const handleUpdateFile = () => {
    if (!filePath.trim() || !commitMessage.trim()) return
    updateFileMutation.mutate({
      path: filePath,
      content: fileContent,
      commitMessage: commitMessage,
    })
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[90vw] max-h-[90vh] overflow-hidden flex flex-col">
        <DialogHeader>
          <DialogTitle>Edit File</DialogTitle>
          <DialogDescription>
            Update the file content with syntax highlighting
          </DialogDescription>
        </DialogHeader>
        <div className="grid gap-4 py-4 flex-1 overflow-auto">
          <div className="grid gap-2">
            <Label htmlFor="edit-file-path">File Path</Label>
            <Input
              id="edit-file-path"
              value={filePath}
              onChange={(e) => setFilePath(e.target.value)}
            />
          </div>
          <div className="grid gap-2 flex-1">
            <Label htmlFor="edit-file-content">Content</Label>
            <CodeEditor
              value={fileContent}
              onChange={setFileContent}
              language={detectLanguageFromPath(filePath)}
              height="500px"
            />
          </div>
          <div className="grid gap-2">
            <Label htmlFor="edit-file-commit">Commit Message</Label>
            <Input
              id="edit-file-commit"
              placeholder="Update file"
              value={commitMessage}
              onChange={(e) => setCommitMessage(e.target.value)}
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => onOpenChange(false)}>
            Cancel
          </Button>
          <Button
            onClick={handleUpdateFile}
            disabled={!filePath.trim() || !commitMessage.trim() || updateFileMutation.isPending}
          >
            {updateFileMutation.isPending ? 'Updating...' : 'Update File'}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
