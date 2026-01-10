import { useState } from "react";
import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { repositoryApi } from "@/lib/api";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { Badge } from "@/components/ui/badge";
import {
  BookOpen,
  GitBranch,
  Plus,
  GitFork,
  Search,
  Folder,
} from "lucide-react";

export const Route = createFileRoute("/repositories/")({
  component: RepositoriesPage,
});

function RepositoriesPage() {
  const queryClient = useQueryClient();
  const [searchQuery, setSearchQuery] = useState("");
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [cloneDialogOpen, setCloneDialogOpen] = useState(false);

  // Form states - updated to use owner/repo format
  const [newRepoOwner, setNewRepoOwner] = useState("");
  const [newRepoName, setNewRepoName] = useState("");
  const [newRepoDescription, setNewRepoDescription] = useState("");
  const [cloneRepoOwner, setCloneRepoOwner] = useState("");
  const [cloneRepoName, setCloneRepoName] = useState("");
  const [cloneRepoSourceUrl, setCloneRepoSourceUrl] = useState("");
  const [cloneRepoDescription, setCloneRepoDescription] = useState("");

  // Query repositories using TanStack Query with repositoryApi
  const {
    data: repositories,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["repositories"],
    queryFn: async () => {
      const response = await repositoryApi.getAllRepositories();
      return response.data as unknown as any[]; // Cast to array since API return type is not properly defined
    },
  });

  // Create repository mutation
  const createMutation = useMutation({
    mutationFn: async (data: { identifier: string; description?: string }) => {
      const response = await repositoryApi.createRepository(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["repositories"] });
      setCreateDialogOpen(false);
      setNewRepoOwner("");
      setNewRepoName("");
      setNewRepoDescription("");
    },
  });

  // Clone repository mutation
  const cloneMutation = useMutation({
    mutationFn: async (data: {
      identifier: string;
      sourceUrl: string;
      description?: string;
    }) => {
      const response = await repositoryApi.cloneRepository(data);
      return response.data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["repositories"] });
      setCloneDialogOpen(false);
      setCloneRepoOwner("");
      setCloneRepoName("");
      setCloneRepoSourceUrl("");
      setCloneRepoDescription("");
    },
  });

  const handleCreateRepository = () => {
    if (!newRepoOwner.trim() || !newRepoName.trim()) return;
    createMutation.mutate({
      identifier: `${newRepoOwner}/${newRepoName}`,
      description: newRepoDescription || undefined,
    });
  };

  const handleCloneRepository = () => {
    if (
      !cloneRepoOwner.trim() ||
      !cloneRepoName.trim() ||
      !cloneRepoSourceUrl.trim()
    )
      return;
    cloneMutation.mutate({
      identifier: `${cloneRepoOwner}/${cloneRepoName}`,
      sourceUrl: cloneRepoSourceUrl,
      description: cloneRepoDescription || undefined,
    });
  };

  const filteredRepositories = repositories?.filter(
    (repo: any) =>
      repo.identifier?.toLowerCase().includes(searchQuery.toLowerCase()) ||
      repo.description?.toLowerCase().includes(searchQuery.toLowerCase()),
  );

  return (
    <div className="min-h-screen bg-background">
      <div className="container py-8 px-4 mx-auto max-w-7xl">
        {/* Header */}
        <div className="flex justify-between items-center mb-8">
          <div>
            <h1 className="mb-2 text-4xl font-bold">Repositories</h1>
            <p className="text-muted-foreground">
              Manage your code repositories
            </p>
          </div>
          <div className="flex gap-2">
            <Dialog open={cloneDialogOpen} onOpenChange={setCloneDialogOpen}>
              <DialogTrigger asChild>
                <Button variant="outline" className="gap-2">
                  <GitFork className="w-4 h-4" />
                  Clone
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-125">
                <DialogHeader>
                  <DialogTitle>Clone Repository</DialogTitle>
                  <DialogDescription>
                    Clone an existing repository from a source URL
                  </DialogDescription>
                </DialogHeader>
                <div className="grid gap-4 py-4">
                  <div className="grid gap-2">
                    <Label htmlFor="clone-owner">Repository Owner</Label>
                    <Input
                      id="clone-owner"
                      placeholder="username"
                      value={cloneRepoOwner}
                      onChange={(e) => setCloneRepoOwner(e.target.value)}
                    />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="clone-name">Repository Name</Label>
                    <Input
                      id="clone-name"
                      placeholder="my-cloned-repo"
                      value={cloneRepoName}
                      onChange={(e) => setCloneRepoName(e.target.value)}
                    />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="clone-source">Source URL</Label>
                    <Input
                      id="clone-source"
                      placeholder="https://github.com/user/repo.git"
                      value={cloneRepoSourceUrl}
                      onChange={(e) => setCloneRepoSourceUrl(e.target.value)}
                    />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="clone-description">
                      Description (Optional)
                    </Label>
                    <Textarea
                      id="clone-description"
                      placeholder="A brief description of this repository"
                      value={cloneRepoDescription}
                      onChange={(e) => setCloneRepoDescription(e.target.value)}
                      rows={3}
                    />
                  </div>
                </div>
                <DialogFooter>
                  <Button
                    variant="outline"
                    onClick={() => setCloneDialogOpen(false)}
                  >
                    Cancel
                  </Button>
                  <Button
                    onClick={handleCloneRepository}
                    disabled={
                      !cloneRepoOwner.trim() ||
                      !cloneRepoName.trim() ||
                      !cloneRepoSourceUrl.trim() ||
                      cloneMutation.isPending
                    }
                  >
                    {cloneMutation.isPending
                      ? "Cloning..."
                      : "Clone Repository"}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>

            <Dialog open={createDialogOpen} onOpenChange={setCreateDialogOpen}>
              <DialogTrigger asChild>
                <Button className="gap-2">
                  <Plus className="w-4 h-4" />
                  New Repository
                </Button>
              </DialogTrigger>
              <DialogContent className="sm:max-w-125">
                <DialogHeader>
                  <DialogTitle>Create New Repository</DialogTitle>
                  <DialogDescription>
                    Create a new empty repository
                  </DialogDescription>
                </DialogHeader>
                <div className="grid gap-4 py-4">
                  <div className="grid gap-2">
                    <Label htmlFor="owner">Repository Owner</Label>
                    <Input
                      id="owner"
                      placeholder="username"
                      value={newRepoOwner}
                      onChange={(e) => setNewRepoOwner(e.target.value)}
                    />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="name">Repository Name</Label>
                    <Input
                      id="name"
                      placeholder="my-awesome-project"
                      value={newRepoName}
                      onChange={(e) => setNewRepoName(e.target.value)}
                    />
                  </div>
                  <div className="grid gap-2">
                    <Label htmlFor="description">Description (Optional)</Label>
                    <Textarea
                      id="description"
                      placeholder="A brief description of this repository"
                      value={newRepoDescription}
                      onChange={(e) => setNewRepoDescription(e.target.value)}
                      rows={3}
                    />
                  </div>
                </div>
                <DialogFooter>
                  <Button
                    variant="outline"
                    onClick={() => setCreateDialogOpen(false)}
                  >
                    Cancel
                  </Button>
                  <Button
                    onClick={handleCreateRepository}
                    disabled={
                      !newRepoOwner.trim() ||
                      !newRepoName.trim() ||
                      createMutation.isPending
                    }
                  >
                    {createMutation.isPending
                      ? "Creating..."
                      : "Create Repository"}
                  </Button>
                </DialogFooter>
              </DialogContent>
            </Dialog>
          </div>
        </div>

        {/* Search Bar */}
        <div className="mb-6">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 w-4 h-4 transform -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search repositories..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="pl-10"
            />
          </div>
        </div>

        {/* Error State */}
        {error && (
          <Card className="border-destructive">
            <CardContent className="pt-6">
              <p className="text-destructive">
                Error loading repositories: {String(error)}
              </p>
            </CardContent>
          </Card>
        )}

        {/* Loading State */}
        {isLoading && (
          <div className="grid gap-4">
            {[1, 2, 3].map((i) => (
              <Card key={i} className="animate-pulse">
                <CardHeader>
                  <div className="mb-2 w-1/3 h-6 rounded bg-muted"></div>
                  <div className="w-2/3 h-4 rounded bg-muted"></div>
                </CardHeader>
              </Card>
            ))}
          </div>
        )}

        {/* Repositories List */}
        {!isLoading && filteredRepositories && (
          <>
            {filteredRepositories.length === 0 ? (
              <Card>
                <CardContent className="py-12 pt-6 text-center">
                  <BookOpen className="mx-auto mb-4 w-12 h-12 text-muted-foreground" />
                  <p className="mb-2 text-lg font-medium">
                    No repositories found
                  </p>
                  <p className="mb-4 text-muted-foreground">
                    {searchQuery
                      ? "Try adjusting your search terms"
                      : "Get started by creating your first repository"}
                  </p>
                  {!searchQuery && (
                    <Button
                      onClick={() => setCreateDialogOpen(true)}
                      className="gap-2"
                    >
                      <Plus className="w-4 h-4" />
                      Create Repository
                    </Button>
                  )}
                </CardContent>
              </Card>
            ) : (
              <div className="grid gap-4">
                {filteredRepositories.map((repo: any) => {
                  // Parse identifier as owner/repo
                  const [owner, repoName] = repo.identifier?.split("/") || [
                    "",
                    "",
                  ];

                  return (
                    <Link
                      key={repo.identifier}
                      to="/repositories/$owner/$repo"
                      params={{ owner, repo: repoName }}
                      search={{ path: undefined }}
                    >
                      <Card className="transition-colors cursor-pointer hover:border-primary">
                        <CardHeader>
                          <div className="flex justify-between items-start">
                            <div className="flex-1">
                              <CardTitle className="flex gap-2 items-center mb-2">
                                <GitBranch className="w-5 h-5 text-muted-foreground" />
                                <span className="hover:underline text-primary">
                                  {repo.identifier}
                                </span>
                              </CardTitle>
                              <CardDescription className="mb-3">
                                {repo.description || "No description provided"}
                              </CardDescription>
                              <div className="flex gap-2 items-center text-sm text-muted-foreground">
                                <Folder className="w-4 h-4" />
                                <code className="py-1 px-2 text-xs rounded bg-muted">
                                  {repo.filesystemPath}
                                </code>
                              </div>
                            </div>
                            <Badge variant="outline">Repository</Badge>
                          </div>
                        </CardHeader>
                      </Card>
                    </Link>
                  );
                })}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
