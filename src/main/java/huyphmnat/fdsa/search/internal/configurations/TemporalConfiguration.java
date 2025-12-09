package huyphmnat.fdsa.search.internal.configurations;

import huyphmnat.fdsa.search.activities.DetectLangActivities;
import huyphmnat.fdsa.search.internal.activities.DetectLangActivitiesImpl;
import huyphmnat.fdsa.search.internal.workflows.IndexSearchWorkflowImpl;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class TemporalConfiguration {
    private final WorkerFactory workerFactory;
    public static final String SEARCH_TASK_QUEUE = "SEARCH_TASK_QUEUE";

    @PostConstruct
    public void init() {
        var worker = workerFactory.newWorker(SEARCH_TASK_QUEUE);
        worker.registerActivitiesImplementations(new DetectLangActivitiesImpl());
        worker.registerWorkflowImplementationTypes(IndexSearchWorkflowImpl.class);
    }
}
