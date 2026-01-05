package huyphmnat.fdsa.shared;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfiguration {

    @Value("${spring.opensearch.host:localhost}")
    private String host;

    @Value("${spring.opensearch.port:9201}")
    private int port;

    @Value("${spring.opensearch.scheme:http}")
    private String scheme;

    @Bean
    public OpenSearchClient openSearchClient() {
        final HttpHost httpHost = new HttpHost(scheme, host, port);

        return new OpenSearchClient(
            ApacheHttpClient5TransportBuilder
                .builder(httpHost)
                .build()
        );
    }
}

