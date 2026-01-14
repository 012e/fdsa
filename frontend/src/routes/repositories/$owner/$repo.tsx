import { createFileRoute, Link, useNavigate } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { repositoryApi } from "@/lib/api";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { ArrowLeft } from "lucide-react";
import { RepositoryHeader } from "./-repository-header";
import { FileNavigator } from "./-file-navigator";
import z from "zod";

export const Route = createFileRoute("/repositories/$owner/$repo")({
  component: RepositoryDetailPage,
  validateSearch: z.object({
    path: z.string().optional().nullable(),
    file: z.string().optional().nullable(),
  }),
});

function RepositoryDetailPage() {
  const { owner, repo } = Route.useParams();
  const navigate = useNavigate();
  const { path: currentPath, file: fileToOpen } = Route.useSearch();

  // Query repository using TanStack Query with repositoryApi
  const identifier = `${owner}/${repo}`;
  const {
    data: repository,
    isLoading,
    error,
  } = useQuery({
    queryKey: ["repository", identifier],
    queryFn: async () => {
      const response = await repositoryApi.getRepository(owner, repo);
      return response.data;
    },
  });

  const handlePathChange = (newPath: string) => {
    navigate({
      to: "/repositories/$owner/$repo",
      params: { owner, repo },
      search: { path: newPath },
    });
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container py-8 px-4 mx-auto max-w-7xl">
          <Card className="animate-pulse">
            <CardHeader>
              <div className="mb-2 w-1/3 h-8 rounded bg-muted"></div>
              <div className="w-2/3 h-4 rounded bg-muted"></div>
            </CardHeader>
          </Card>
        </div>
      </div>
    );
  }

  if (error || !repository) {
    return (
      <div className="min-h-screen bg-background">
        <div className="container py-8 px-4 mx-auto max-w-7xl">
          <Card className="border-destructive">
            <CardContent className="pt-6">
              <p className="text-destructive">
                Error loading repository:{" "}
                {String(error) || "Repository not found"}
              </p>
            </CardContent>
          </Card>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <div className="container py-8 px-4 mx-auto max-w-7xl">
        {/* Back Button */}
        <Link
          to="/repositories"
          className="inline-flex gap-2 items-center mb-6 text-muted-foreground hover:text-foreground"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to repositories
        </Link>

        {/* Repository Header */}
        <RepositoryHeader repository={repository} />

        {/* File Navigator */}
        <FileNavigator
          owner={owner}
          repo={repo}
          currentPath={currentPath ?? ""}
          onPathChange={handlePathChange}
          fileToOpen={fileToOpen ?? undefined}
        />
      </div>
    </div>
  );
}
