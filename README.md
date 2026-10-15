# GameVault

Loja digital de jogos. O usuário navega pelo catálogo, compra com uma carteira virtual, recebe os
títulos em uma biblioteca pessoal e avalia os que possui. Publicadoras cadastram e gerenciam os
próprios jogos, com promoções por período. Administradores mantêm categorias e usuários.

Trabalho da disciplina de Desenvolvimento Backend com Spring Boot — UNESC.
Feito por Caio Marcon Hobold e Gabriel Gonçalves Flôr.

## Tecnologias

Backend em Java 21 com Spring Boot 3.5, Spring Data JPA, Spring Security com JWT e Lombok.
Banco PostgreSQL 17 versionado com Flyway. Documentação da API em Swagger/OpenAPI.
Frontend em React 19 com TypeScript e Vite.

## Como rodar

Você vai precisar de JDK 21, Node 22 e um PostgreSQL. O Docker é opcional: serve só para subir o
banco sem instalar nada.

### 1. Banco

Com Docker:

```bash
cp .env.example .env
docker compose up -d
```

Sem Docker, em um PostgreSQL já instalado:

```sql
CREATE ROLE gamevault WITH LOGIN PASSWORD 'gamevault';
CREATE DATABASE gamevault OWNER gamevault;
```

As tabelas e os dados iniciais são criados pelo Flyway na primeira vez que a API sobe.

### 2. Backend

```bash
cd GameVaultBack
./mvnw spring-boot:run
```

A API responde em `http://localhost:8080` e o Swagger fica em
`http://localhost:8080/swagger-ui.html`.

Se o `java -version` da sua máquina não for 21, aponte o JDK antes de rodar:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v21)   # macOS
```

No Windows o comando do Maven é `.\mvnw.cmd spring-boot:run`.

### 3. Frontend

Em outro terminal, com a API no ar:

```bash
cd GameVaultFront
cp .env.example .env
npm install
npm run dev
```

A interface abre em `http://localhost:5173`.

## Credenciais

Criadas pela migration `V2`, junto com dez jogos de exemplo.

| E-mail | Senha | Papel | O que dá para fazer |
|---|---|---|---|
| `admin@gamevault.dev` | `admin123` | ADMIN | gerenciar usuários e categorias, além de tudo o que os outros fazem |
| `contato@pixelforge.dev` | `publi123` | PUBLICADORA | publicar jogos, criar promoções e acompanhar as próprias vendas |
| `jogador@gamevault.dev` | `user123` | USUARIO | comprar, manter a biblioteca e avaliar — começa com R$ 500 de saldo |

## Estrutura de pastas

```
GameVault/
├── GameVaultBack/      API REST
├── GameVaultFront/     interface web
├── docker-compose.yml  PostgreSQL e pgAdmin
└── .env.example        modelo das variáveis de ambiente
```

O backend segue arquitetura em camadas:

```
GameVaultBack/src/main/
├── java/br/edu/unesc/gamevault/
│   ├── config/       SecurityConfig, CorsConfig, OpenApiConfig
│   ├── security/     geração e validação do JWT, filtro e UserDetails
│   ├── controller/   só orquestração HTTP
│   ├── service/      toda a regra de negócio
│   ├── repository/   interfaces JpaRepository
│   ├── entity/       entidades JPA e enums
│   ├── dto/          request e response, com Bean Validation
│   ├── mapper/       conversão entre entidade e DTO
│   └── exception/    exceções próprias e GlobalExceptionHandler
└── resources/db/migration/   scripts do Flyway
```

No frontend, `src/api` concentra as chamadas HTTP, `src/paginas` tem uma página por rota,
`src/componentes` as peças de interface e `src/contextos` o estado de sessão e carrinho.
Detalhes em [GameVaultFront/README.md](GameVaultFront/README.md).

## Usando a API

Todos os exemplos abaixo são respostas reais da aplicação rodando local.

### Autenticar

```bash
curl -X POST http://localhost:8080/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"jogador@gamevault.dev","senha":"user123"}'
```

```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJzdWIiOiJqb2dhZG9yQGdhbWV...",
  "tipo": "Bearer",
  "expiraEm": "2026-10-16T14:35:53Z",
  "usuario": {
    "id": 3,
    "nome": "Jogador de Teste",
    "email": "jogador@gamevault.dev",
    "role": "USUARIO",
    "saldo": 500.00,
    "ativo": true
  }
}
```

O token vale 24 horas e vai no cabeçalho `Authorization: Bearer {token}` das rotas protegidas.

### Listar o catálogo

Rota pública, paginada. Aceita `titulo`, `categoriaId`, `page`, `size` e `sort`.

```bash
curl "http://localhost:8080/jogos?titulo=colonia&size=2&sort=titulo,asc"
```

```json
{
  "content": [
    {
      "id": 3,
      "titulo": "Colonia Orbital",
      "descricao": "Simulador de gestao de uma colonia espacial: recursos, moral da tripulacao e imprevistos.",
      "preco": 89.90,
      "dataLancamento": "2023-11-20",
      "publicadora": { "id": 2, "nome": "Pixel Forge Studios" },
      "notaMedia": 0.00,
      "totalAvaliacoes": 0,
      "ativo": true,
      "categorias": [
        { "id": 4, "nome": "Estrategia", "descricao": "Planejamento, gerenciamento de recursos e taticas" },
        { "id": 9, "nome": "Simulacao", "descricao": "Simuladores de atividades e sistemas reais" }
      ]
    }
  ],
  "totalElements": 1,
  "totalPages": 1,
  "number": 0,
  "size": 2
}
```

### Comprar um jogo

O corpo leva só a lista de jogos: quem é o dono do pedido vem do token.

```bash
curl -X POST http://localhost:8080/pedidos \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"itens":[{"jogoId":1}]}'
```

```json
{
  "id": 1,
  "usuario": { "id": 3, "nome": "Jogador de Teste" },
  "dataPedido": "2026-10-15T11:35:53.359837",
  "status": "PENDENTE",
  "valorTotal": 199.90,
  "itens": [
    {
      "id": 1,
      "jogo": { "id": 1, "titulo": "Sombras de Aldoria", "preco": 199.90, "notaMedia": 0.00, "ativo": true },
      "precoUnitario": 199.90,
      "descontoAplicado": 0.00,
      "subtotal": 199.90
    }
  ]
}
```

O pedido nasce `PENDENTE`. É o pagamento que debita o saldo e libera os jogos na biblioteca:

```bash
curl -X POST http://localhost:8080/pedidos/1/pagar -H "Authorization: Bearer $TOKEN"
```

```json
{ "id": 1, "status": "PAGO", "valorTotal": 199.90 }
```

### Erros

Toda falha devolve o mesmo formato, com o código HTTP correspondente.

Regra de negócio violada, `422`:

```json
{
  "timestamp": "2026-10-15T11:35:53.441463",
  "status": 422,
  "erro": "Regra de negócio violada",
  "mensagem": "O jogo 'Sombras de Aldoria' já está na biblioteca deste usuário",
  "path": "/pedidos"
}
```

Validação de campo, `400`, com o mapa do que está errado:

```json
{
  "timestamp": "2026-10-15T11:35:53.476762",
  "status": 400,
  "erro": "Erro de validação",
  "mensagem": "Um ou mais campos estão inválidos",
  "path": "/avaliacoes",
  "campos": { "nota": "A nota máxima é 5" }
}
```

Requisição sem token em rota protegida, `401`:

```json
{
  "timestamp": "2026-10-15T11:35:53.45303",
  "status": 401,
  "erro": "Não autenticado",
  "mensagem": "É necessário enviar um token JWT válido no cabeçalho Authorization",
  "path": "/pedidos/meus"
}
```

Os demais códigos seguem a mesma estrutura: `403` para papel sem permissão ou recurso de outro
usuário, `404` para recurso inexistente e `409` para conflito, como e-mail repetido.

### Demais endpoints

A lista completa, com descrição e exemplos de cada campo, está no Swagger em
`http://localhost:8080/swagger-ui.html`. Para testar as rotas protegidas por lá, faça o login,
copie o `token` da resposta e cole no botão **Authorize**, sem o prefixo `Bearer`.
