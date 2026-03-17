package com.buyglimmer.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI buyGlimmerOpenApi() {
        return new OpenAPI().info(new Info()
                .title("BuyGlimmer Backend API")
                .description("Spring Boot backend scaffold for the BuyGlimmer storefront")
                .version("v1")
                .contact(new Contact().name("BuyGlimmer").email("support@buyglimmer.com")));
    }
}