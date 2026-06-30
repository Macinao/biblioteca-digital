package com.digital.biblioteca.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartTecaOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("SmartTeca API")
                .description("API do sistema de biblioteca acadêmica SmartTeca — TCC. "
                        + "Acervo (livros, livros públicos, TCCs, materiais), reservas, "
                        + "usuários e assistente de IA.")
                .version("v1"));
    }
}
