package huyphmnat.fdsa.search.internal.workflows;

import huyphmnat.fdsa.search.activities.DetectLangActivities;
import huyphmnat.fdsa.search.workflows.IndexSearchWorkflow;
import huyphmnat.fdsa.snippet.dtos.Snippet;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

public class IndexSearchWorkflowImpl implements IndexSearchWorkflow {
    private final DetectLangActivities detectLangActivities = Workflow.newActivityStub(
                    DetectLangActivities.class,
                    ActivityOptions.newBuilder().validateAndBuildWithDefaults());

    @Override
    public void indexSnippet(Snippet snippet) {
        String lang = detectLangActivities.detectLanguage(snippet.getCode());
    }
}
