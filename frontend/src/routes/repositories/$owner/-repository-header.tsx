import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { GitBranch, MoreVertical, Trash2 } from "lucide-react";
import { Repository } from "@/lib/generated";
import {
  ContextMenu,
  ContextMenuContent,
  ContextMenuItem,
  ContextMenuTrigger,
} from "@/components/ui/context-menu";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import { useState } from "react";
import { toast } from "sonner";
import { useNavigate } from "@tanstack/react-router";

interface RepositoryHeaderProps {
  repository: Repository;
}

export function RepositoryHeader({ repository }: RepositoryHeaderProps) {
  const [owner, repo] = (repository.identifier || "/").split("/");
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const navigate = useNavigate();

  const handleDeleteRepository = async () => {
    // TODO: Implement when backend API is available
    toast.error("Delete repository API not yet implemented", {
      description: "This feature requires a backend endpoint for deleting repositories",
    });
    setDeleteDialogOpen(false);
    // When API is available:
    // await repositoryApi.deleteRepository(owner, repo)
    // navigate({ to: '/repositories' })
  };

  return (
    <>
      <Card className="mb-6">
        <CardHeader>
          <div className="flex justify-between items-start">
            <div className="flex-1">
              <CardTitle className="flex gap-3 items-center mb-3">
                <GitBranch className="w-6 h-6 text-muted-foreground" />
                <span className="text-3xl">
                  {owner}/{repo}
                </span>
                <Badge variant="secondary">Repository</Badge>
              </CardTitle>
              <CardDescription className="mb-4 text-base">
                {repository.description || "No description provided"}
              </CardDescription>
            </div>
            <ContextMenu>
              <ContextMenuTrigger asChild>
                <Button variant="ghost" size="icon" className="h-8 w-8">
                  <MoreVertical className="h-4 w-4" />
                </Button>
              </ContextMenuTrigger>
              <ContextMenuContent>
                <ContextMenuItem
                  variant="destructive"
                  onClick={() => setDeleteDialogOpen(true)}
                >
                  <Trash2 />
                  Delete repository
                </ContextMenuItem>
              </ContextMenuContent>
            </ContextMenu>
          </div>
        </CardHeader>
      </Card>

      <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Repository</DialogTitle>
            <DialogDescription>
              Are you sure you want to delete{" "}
              <span className="font-semibold">{repository.identifier}</span>?
              <span className="block mt-2 text-destructive">
                Warning: This action cannot be undone. All files and history will be permanently deleted.
              </span>
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteDialogOpen(false)}>
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDeleteRepository}>
              Delete Repository
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
