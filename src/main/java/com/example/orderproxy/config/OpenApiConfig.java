package com.example.orderproxy.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Service API")
                        .description("CRUD API для управления заказами (JPA / JDBC)")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Jeexike")
                                .email("jeex1ke@gmail.com"))
                );
    }
}
