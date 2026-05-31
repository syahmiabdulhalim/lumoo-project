package com.example.lumoo.infrastructure.web;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lumooApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LUMOO API")
                        .version("1.0")
                        .description("LUMOO Gambia — Construction Materials Marketplace")
                        .contact(new Contact()
                                .name("LUMOO Support")
                                .email("info@lumoo.gm")));
    }
}
