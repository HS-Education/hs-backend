package com.hs.hstesis.shared.infrastructure.documentation.openapi.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
public class OpenApiConfiguration {

    @Bean
    public OpenAPI hsOpenApi() {

        var openApi = new OpenAPI();

        openApi
                .info(new Info()
                        .title("HS API")
                        .description("HS REST API documentation")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0")
                                .url("http://springdoc.org")));

        return openApi;
    }
}