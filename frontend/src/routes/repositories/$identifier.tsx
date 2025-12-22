import { useState } from 'react'
import { createFileRoute, Link } from '@tanstack/react-router'
import { useQuery, useMutation } from '@tanstack/react-query'
import { graphqlClient } from '@/lib/graphql-client'
import {
  GET_REPOSITORY,
  ADD_REPOSITORY_FILE,
  UPDATE_REPOSITORY_FILE,
  DELETE_REPOSITORY_FILE,
  CREATE_REPOSITORY_FOLDER,
  DELETE_REPOSITORY_FOLDER,
} from '@/graphql/repository-queries'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { 
  GitBranch, 
  ArrowLeft, 
  Folder, 
  Plus, 
  FolderPlus,
} from 'lucide-react'

export const Route = createFileRoute('/repositories/$identifier')({
  component: RepositoryDetailPage,
})

type Repository = {
  identifier: string
  description: string | null
  filesystemPath: string
}

function RepositoryDetailPage() {
  const { identifier } = Route.useParams()

  // Dialog states
  const [addFileDialogOpen, setAddFileDialogOpen] = useState(false)
  const [addFolderDialogOpen, setAddFolderDialogOpen] = useState(false)
  const [editFileDialogOpen, setEditFileDialogOpen] = useState(false)

  // Form states
  const [newFilePath, setNewFilePath] = useState('')
  const [newFileContent, setNewFileContent] = useState('')
  const [newFileCommitMessage, setNewFileCommitMessage] = useState('')
  
  const [newFolderPath, setNewFolderPath] = useState('')
  const [newFolderCommitMessage, setNewFolderCommitMessage] = useState('')

  const [editFilePath, setEditFilePath] = useState('')
  const [editFileContent, setEditFileContent] = useState('')
  const [editFileCommitMessage, setEditFileCommitMessage] = useState('')

  // Query repository
  const { data: repository, isLoading, error } = useQuery({
    queryKey: ['repository', identifier],
    queryFn: async () => {
      const data = await graphqlClient.request(GET_REPOSITORY, { identifier })
      return data.repository as Repository
    },
  })

  // Add file mutation
  const addFileMutation = useMutation({
    mutationFn: async (input: { 
      repositoryId: string
      path: string
      content: string
      commitMessage: string
    }) => {
      return await graphqlClient.request(ADD_REPOSITORY_FILE, { input })
    },
    onSuccess: () => {
      setAddFileDialogOpen(false)
      setNewFilePath('')
      setNewFileContent('')
      setNewFileCommitMessage('')
    },
  })

  // Update file mutation
  const updateFileMutation = useMutation({
    mutationFn: async (input: { 
      repositoryId: string
      path: string
      content: string
      commitMessage: string
    }) => {
      return await graphqlClient.request(UPDATE_REPOSITORY_FILE, { input })
    },
    onSuccess: () => {
      setEditFileDialogOpen(false)
      setEditFilePath('')
      setEditFileContent('')
      setEditFileCommitMessage('')
    },
  })

  // Delete file mutation
  const deleteFileMutation = useMutation({
    mutationFn: async (input: { 
      repositoryId: string
      path: string
      commitMessage: string
    }) => {
      return await graphqlClient.request(DELETE_REPOSITORY_FILE, { input })
    },
  })

  // Create folder mutation
  const createFolderMutation = useMutation({
    mutationFn: async (input: { 
      repositoryId: string
      path: string
      commitMessage: string
    }) => {
      return await graphqlClient.request(CREATE_REPOSITORY_FOLDER, { input })
    },
    onSuccess: () => {
      setAddFolderDialogOpen(false)
      setNewFolderPath('')
      setNewFolderCommitMessage('')
    },
  })

  // Delete folder mutation
  const deleteFolderMutation = useMutation({
    mutationFn: async (input: { 
      repositoryId: string
      path: string
      commitMessage: string
    }) => {
      return await graphqlClient.request(DELETE_REPOSITORY_FOLDER, { input })
    },
  })

  const handleAddFile = () => {
    if (!repository || !newFilePath.trim() || !newFileCommitMessage.trim()) return
    addFileMutation.mutate({
      repositoryId: repository.identifier,
      path: newFilePath,
      content: newFileContent,
      commitMessage: newFileCommitMessage,
    })
  }

  const handleUpdateFile = () => {
    if (!repository || !editFilePath.trim() || !editFileCommitMessage.trim()) return
    updateFileMutation.mutate({
      repositoryId: repository.identifier,
      path: editFilePath,
      content: editFileContent,
      commitMessage: editFileCommitMessage,
    })
  }

  const handleCreateFolder = () => {
    if (!repository || !newFolderPath.trim() || !newFolderCommitMessage.trim()) return
    createFolderMutation.mutate({
      repositoryId: repository.identifier,
      path: newFolderPath,
      commitMessage: newFolderCommitMessage,
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
                Error loading repository: {error?.message || 'Repository not found'}
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
        <Card className="mb-6">
          <CardHeader>
            <div className="flex items-start justify-between">
              <div className="flex-1">
                <CardTitle className="flex items-center gap-3 mb-3">
                  <GitBranch className="w-6 h-6 text-muted-foreground" />
                  <span className="text-3xl">{repository.identifier}</span>
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

        {/* File Operations */}
        <Card>
          <CardHeader>
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-xl">Repository Files</CardTitle>
                <CardDescription>Manage files and folders in this repository</CardDescription>
              </div>
              <div className="flex gap-2">
                <Dialog open={addFolderDialogOpen} onOpenChange={setAddFolderDialogOpen}>
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
                        <Label htmlFor="folder-path">Folder Path</Label>
                        <Input
                          id="folder-path"
                          placeholder="src/components"
                          value={newFolderPath}
                          onChange={(e) => setNewFolderPath(e.target.value)}
                        />
                      </div>
                      <div className="grid gap-2">
                        <Label htmlFor="folder-commit">Commit Message</Label>
                        <Input
                          id="folder-commit"
                          placeholder="Create new folder"
                          value={newFolderCommitMessage}
                          onChange={(e) => setNewFolderCommitMessage(e.target.value)}
                        />
                      </div>
                    </div>
                    <DialogFooter>
                      <Button variant="outline" onClick={() => setAddFolderDialogOpen(false)}>
                        Cancel
                      </Button>
                      <Button
                        onClick={handleCreateFolder}
                        disabled={!newFolderPath.trim() || !newFolderCommitMessage.trim() || createFolderMutation.isPending}
                      >
                        {createFolderMutation.isPending ? 'Creating...' : 'Create Folder'}
                      </Button>
                    </DialogFooter>
                  </DialogContent>
                </Dialog>

                <Dialog open={addFileDialogOpen} onOpenChange={setAddFileDialogOpen}>
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
                        <Label htmlFor="file-path">File Path</Label>
                        <Input
                          id="file-path"
                          placeholder="src/index.ts"
                          value={newFilePath}
                          onChange={(e) => setNewFilePath(e.target.value)}
                        />
                      </div>
                      <div className="grid gap-2">
                        <Label htmlFor="file-content">Content</Label>
                        <Textarea
                          id="file-content"
                          placeholder="File content..."
                          value={newFileContent}
                          onChange={(e) => setNewFileContent(e.target.value)}
                          rows={8}
                          className="font-mono text-sm"
                        />
                      </div>
                      <div className="grid gap-2">
                        <Label htmlFor="file-commit">Commit Message</Label>
                        <Input
                          id="file-commit"
                          placeholder="Add new file"
                          value={newFileCommitMessage}
                          onChange={(e) => setNewFileCommitMessage(e.target.value)}
                        />
                      </div>
                    </div>
                    <DialogFooter>
                      <Button variant="outline" onClick={() => setAddFileDialogOpen(false)}>
                        Cancel
                      </Button>
                      <Button
                        onClick={handleAddFile}
                        disabled={!newFilePath.trim() || !newFileCommitMessage.trim() || addFileMutation.isPending}
                      >
                        {addFileMutation.isPending ? 'Adding...' : 'Add File'}
                      </Button>
                    </DialogFooter>
                  </DialogContent>
                </Dialog>
              </div>
            </div>
          </CardHeader>
          <Separator />
          <CardContent className="pt-6">
            <div className="space-y-2">
              <p className="text-sm text-muted-foreground">
                Use the buttons above to manage files and folders in this repository.
              </p>
              <p className="text-sm text-muted-foreground">
                File operations are committed directly to the repository with the provided commit messages.
              </p>
            </div>
          </CardContent>
        </Card>

        {/* Edit File Dialog */}
        <Dialog open={editFileDialogOpen} onOpenChange={setEditFileDialogOpen}>
          <DialogContent className="sm:max-w-125">
            <DialogHeader>
              <DialogTitle>Edit File</DialogTitle>
              <DialogDescription>
                Update the file content
              </DialogDescription>
            </DialogHeader>
            <div className="grid gap-4 py-4">
              <div className="grid gap-2">
                <Label htmlFor="edit-file-path">File Path</Label>
                <Input
                  id="edit-file-path"
                  value={editFilePath}
                  onChange={(e) => setEditFilePath(e.target.value)}
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-file-content">Content</Label>
                <Textarea
                  id="edit-file-content"
                  value={editFileContent}
                  onChange={(e) => setEditFileContent(e.target.value)}
                  rows={8}
                  className="font-mono text-sm"
                />
              </div>
              <div className="grid gap-2">
                <Label htmlFor="edit-file-commit">Commit Message</Label>
                <Input
                  id="edit-file-commit"
                  placeholder="Update file"
                  value={editFileCommitMessage}
                  onChange={(e) => setEditFileCommitMessage(e.target.value)}
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setEditFileDialogOpen(false)}>
                Cancel
              </Button>
              <Button
                onClick={handleUpdateFile}
                disabled={!editFilePath.trim() || !editFileCommitMessage.trim() || updateFileMutation.isPending}
              >
                {updateFileMutation.isPending ? 'Updating...' : 'Update File'}
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </div>
  )
}
