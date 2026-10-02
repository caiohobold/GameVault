package br.edu.unesc.gamevault.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

@Configuration
public class OpenApiConfig {
    private static final String ESQUEMA_JWT = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("GameVault API")
                        .version("v1")
                        .description("""
                                Plataforma de distribuição digital de jogos.

                                Autenticação: faça POST em /auth/login, copie o campo `token` da
                                resposta e informe-o no botão **Authorize** (sem o prefixo Bearer).

                                Credenciais de teste:
                                - admin@gamevault.dev / admin123 (ADMIN)
                                - contato@pixelforge.dev / publi123 (PUBLICADORA)
                                - jogador@gamevault.dev / user123 (USUARIO)
                                """))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components().addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                        .name(ESQUEMA_JWT)
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT obtido em /auth/login")));
    }
}
