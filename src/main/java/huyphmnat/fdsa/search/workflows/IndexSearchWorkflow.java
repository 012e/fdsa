package huyphmnat.fdsa.search.workflows;

import huyphmnat.fdsa.snippet.dtos.Snippet;
import huyphmnat.fdsa.snippet.dtos.SnippetCreatedEvent;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface IndexSearchWorkflow {
    @WorkflowMethod
    void indexSnippet(Snippet snippet); // may be add options?
}