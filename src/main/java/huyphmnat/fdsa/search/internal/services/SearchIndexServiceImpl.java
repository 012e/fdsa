package huyphmnat.fdsa.search.internal.services;

import huyphmnat.fdsa.search.internal.configurations.TemporalConfiguration;
import huyphmnat.fdsa.search.internal.services.interfaces.SearchIndexService;
import huyphmnat.fdsa.search.workflows.IndexSearchWorkflow;
import huyphmnat.fdsa.snippet.dtos.Snippet;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchIndexServiceImpl implements SearchIndexService {
    private final WorkflowClient workflowClient;

    public void startIndexingSnippet(Snippet snippet) {
        var workflow = workflowClient.newWorkflowStub(
                IndexSearchWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TemporalConfiguration.SEARCH_TASK_QUEUE)
                        .setWorkflowId(UUID.randomUUID().toString())
                        .build()
        );
        workflow.indexSnippet(snippet);
    }
}
