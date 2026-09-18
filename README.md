# GameVault

Loja digital de jogos. O usuário navega pelo catálogo, compra usando uma carteira virtual,
recebe os jogos em uma biblioteca pessoal e avalia os que possui. Publicadoras cadastram e
gerenciam os próprios jogos.

Trabalho da disciplina de Desenvolvimento Backend com Spring Boot (UNESC).

## Backend

Java 21 + Spring Boot 3.5 + PostgreSQL. Precisa de JDK 21 e Docker.

```bash
cp .env.example .env
docker compose up -d

cd GameVaultBack
./mvnw spring-boot:run
```

API em `http://localhost:8080`, Swagger em `http://localhost:8080/swagger-ui.html`.

## Frontend

React + Vite. Precisa de Node 18+ e do backend rodando.

```bash
cd GameVaultFront
npm install
npm run dev
```

Interface em `http://localhost:5173`.
