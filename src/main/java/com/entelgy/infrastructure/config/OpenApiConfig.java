package com.entelgy.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuración de OpenAPI/Swagger para la documentación de la API
 * 
 * Accesos:
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - API Docs: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("POC 1 - JOOQ + PostgreSQL API")
                        .version("1.0.0")
                        .description("""
                                API REST para POC de JOOQ con PostgreSQL.
                                
                                Incluye endpoints para:
                                - Gestión de contratos (CRUD)
                                - Reportes con JOINs avanzados
                                - Consultas nativas
                                - Procedimientos almacenados
                                
                                Tecnologías: Spring Boot 3.5.6, JOOQ 3.19, PostgreSQL, Java 21
                                """)
                        .contact(new Contact()
                                .name("Entelgy")
                                .email("support@entelgy.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Servidor Local"),
                        new Server()
                                .url("https://api-dev.entelgy.com")
                                .description("Servidor de Desarrollo")));
    }
}
