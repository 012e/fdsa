package huyphmnat.fdsa.shared;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {
    @Value("${spring.springdoc.OAuthFlow.authorizationUrl}")
    private String authorizationUrl;

    @Value("${spring.springdoc.OAuthFlow.tokenUrl}")
    private String tokenUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addSecurityItem(new SecurityRequirement().addList("Oauth2"))
                .components(new Components()
                        .addSecuritySchemes("Oauth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.OAUTH2)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .flows(new OAuthFlows()
                                                .authorizationCode(new OAuthFlow()
                                                        .authorizationUrl(authorizationUrl)
                                                        .tokenUrl(tokenUrl)
                                                        .scopes(new Scopes()
                                                                .addString("openid", "openid")
                                                                .addString("email", "email")
                                                                .addString("profile", "profile"))
                                                )
                                        )
                        )
                );
    }
}
