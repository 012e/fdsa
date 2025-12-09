package huyphmnat.fdsa.shared;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowConfiguration {
    @Value("spring.temporal.url")
    private String temporalUrl;

    @Bean
    public WorkflowServiceStubs workflowServiceStubs() {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget(temporalUrl)
                        .build()
        );
    }

    @Bean
    public WorkflowClient workflowClient() {
        return WorkflowClient.newInstance(workflowServiceStubs());
    }

    @Bean
    public WorkerFactory workerFactory() {
        return WorkerFactory.newInstance(workflowClient());
    }
}
