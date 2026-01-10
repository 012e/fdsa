import {
  Card,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { GitBranch, Folder } from "lucide-react";
import { Repository } from "@/lib/generated";

interface RepositoryHeaderProps {
  repository: Repository;
}

export function RepositoryHeader({ repository }: RepositoryHeaderProps) {
  const [owner, repo] = (repository.identifier || "/").split("/");

  return (
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
        </div>
      </CardHeader>
    </Card>
  );
}
