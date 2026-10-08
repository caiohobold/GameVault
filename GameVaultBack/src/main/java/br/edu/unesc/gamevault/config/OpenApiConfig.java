package br.edu.unesc.gamevault.config;

import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.edu.unesc.gamevault.exception.ErroResposta;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

@Configuration
public class OpenApiConfig {
    public static final String ESQUEMA_JWT = "bearerAuth";

    private static final String REF_ERRO = "#/components/schemas/ErroResposta";

    @Value("${server.port:8080}")
    private int porta;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(informacoes())
                .servers(List.of(new Server()
                        .url("http://localhost:" + porta)
                        .description("Ambiente local de desenvolvimento")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .name(ESQUEMA_JWT)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtido em POST /auth/login")));
    }

    private Info informacoes() {
        return new Info()
                .title("GameVault API")
                .version("1.0.0")
                .description("""
                        API REST da **GameVault**, uma plataforma de distribuição digital de jogos.

                        ## Como autenticar

                        1. Chame `POST /auth/login` com um dos usuários de teste abaixo.
                        2. Copie o campo `token` da resposta.
                        3. Clique em **Authorize**, no topo desta página, e cole o token — sem o prefixo `Bearer`.
                        4. A partir daí o Swagger envia o cabeçalho `Authorization` em todas as chamadas.

                        ## Usuários de teste

                        | E-mail | Senha | Papel | O que pode fazer |
                        |---|---|---|---|
                        | `admin@gamevault.dev` | `admin123` | ADMIN | tudo, inclusive gerenciar usuários e categorias |
                        | `contato@pixelforge.dev` | `publi123` | PUBLICADORA | publicar jogos, criar promoções e ver as próprias vendas |
                        | `jogador@gamevault.dev` | `user123` | USUARIO | comprar, manter a biblioteca e avaliar |

                        ## Convenções

                        - **O dono da operação vem sempre do token**, nunca do corpo da requisição.
                        - Listagens são paginadas: use `page`, `size` e `sort` (exemplo: `sort=titulo,asc`).
                        - Valores monetários são decimais com duas casas.
                        - Erros seguem sempre o mesmo formato, descrito no schema `ErroResposta`.

                        ## Códigos de erro

                        | Código | Quando acontece |
                        |---|---|
                        | 400 | corpo inválido ou validação de campo falhou |
                        | 401 | token ausente, expirado ou inválido |
                        | 403 | papel sem permissão, ou o recurso pertence a outro usuário |
                        | 404 | recurso não encontrado |
                        | 409 | conflito, como e-mail repetido ou avaliação duplicada |
                        | 422 | regra de negócio violada, como saldo insuficiente |
                        """)
                .contact(new Contact()
                        .name("Caio Marcon Hobold e Gabriel Gonçalves Flôr")
                        .url("https://github.com/caiohobold/GameVault"))
                .license(new License().name("Uso acadêmico — UNESC"));
    }

    @Bean
    public OpenApiCustomizer respostasDeErroPadrao() {
        return openApi -> {
            registrarSchemaDeErro(openApi);

            openApi.getPaths().values().forEach(caminho -> caminho.readOperations().forEach(operacao -> {
                boolean rotaPublica = operacao.getSecurity() != null && operacao.getSecurity().isEmpty();

                if (!rotaPublica) {
                    adicionar(operacao.getResponses(), "401",
                            "Token ausente, expirado ou inválido");
                    adicionar(operacao.getResponses(), "403",
                            "O papel do usuário não permite esta operação, ou o recurso pertence a outro usuário");
                }

                adicionar(operacao.getResponses(), "500", "Erro inesperado no servidor");
            }));
        };
    }

    private void registrarSchemaDeErro(OpenAPI openApi) {
        if (openApi.getComponents().getSchemas() != null
                && openApi.getComponents().getSchemas().containsKey("ErroResposta")) {
            return;
        }

        ModelConverters.getInstance()
                .readAll(ErroResposta.class)
                .forEach(openApi.getComponents()::addSchemas);
    }

    private void adicionar(io.swagger.v3.oas.models.responses.ApiResponses respostas, String codigo,
            String descricao) {
        if (respostas.containsKey(codigo)) {
            return;
        }

        respostas.addApiResponse(codigo, new ApiResponse()
                .description(descricao)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref(REF_ERRO)))));
    }
}
