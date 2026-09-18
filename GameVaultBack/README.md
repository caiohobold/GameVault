# GameVaultBack

API REST do GameVault — **Java 21 + Spring Boot 3.5**.

Instruções de execução no [README da raiz](../README.md).

## Estrutura de pacotes

```
br.edu.unesc.gamevault
├── config/           # SecurityConfig, OpenApiConfig, CorsConfig
├── security/         # JwtService, JwtAuthenticationFilter, UserDetailsServiceImpl
├── controller/       # apenas orquestração HTTP, sem regra de negócio
├── service/          # toda a regra de negócio vive aqui
├── repository/       # interfaces JpaRepository
├── entity/           # entidades JPA
│   └── enums/        # Role, StatusPedido
├── dto/
│   ├── request/      # DTOs de entrada, com Bean Validation
│   └── response/     # DTOs de saída, nunca expõem senha
├── mapper/           # conversão entity <-> DTO
└── exception/        # exceções customizadas + GlobalExceptionHandler
```

`src/main/resources/db/migration` guarda as migrations Flyway
(`V1__criar_tabelas.sql`, `V2__inserir_dados_iniciais.sql`, ...).
**Migration já aplicada nunca é editada** — cria-se a próxima versão.

## Comandos

```bash
./mvnw clean verify
./mvnw spring-boot:run
```

Requer **JDK 21**. Se o `java -version` da máquina for outro:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v21)
```
