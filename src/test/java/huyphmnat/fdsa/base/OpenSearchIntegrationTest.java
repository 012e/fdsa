package huyphmnat.fdsa.base;

import org.opensearch.testcontainers.OpenSearchContainer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest
@ActiveProfiles({"integration-testing", "opensearch-integration-testing"})
public class OpenSearchIntegrationTest extends BaseIntegrationTest {
    @Container
    static GenericContainer<?> openSearchContainer = new OpenSearchContainer<>(DockerImageName.parse("opensearchproject/opensearch:3.4.0"));

    private static void setupOpenSearch(DynamicPropertyRegistry registry) {
        registry.add("spring.opensearch.host", openSearchContainer::getHost);
        registry.add("spring.opensearch.port", openSearchContainer::getFirstMappedPort);
    }

    @DynamicPropertySource
    public static void properties(DynamicPropertyRegistry registry) {
        setupOpenSearch(registry);
    }
}
