# GameVaultFront

Interface web do GameVault — **React 19 + TypeScript + Vite**, consumindo a API em
`../GameVaultBack`.

## Como rodar

Requer **Node 22** (há um `.nvmrc` no diretório) e o backend no ar em `http://localhost:8080`.

```bash
cp .env.example .env
npm install
npm run dev
```

A interface sobe em `http://localhost:5173`, origem já liberada no CORS do backend.

| Script | O que faz |
|---|---|
| `npm run dev` | servidor de desenvolvimento com recarga automática |
| `npm run build` | checagem de tipos (`tsc -b`) e build de produção em `dist/` |
| `npm run preview` | serve o build de produção localmente |
| `npm run lint` | análise estática com oxlint |

O endereço da API vem de `VITE_API_URL`. Para apontar para outro host, basta alterar o `.env`.

## Credenciais de teste

Criadas pela migration `V2` do backend.

| E-mail | Senha | O que dá para ver |
|---|---|---|
| `jogador@gamevault.dev` | `user123` | catálogo, carrinho, biblioteca, avaliações |
| `contato@pixelforge.dev` | `publi123` | painel da publicadora, vendas, promoções |
| `admin@gamevault.dev` | `admin123` | painel administrativo, usuários, categorias |

## Arquitetura

```
src/
├── api/            uma função por endpoint, agrupada por recurso
│   └── cliente.ts  instância do axios, injeção do token e tradução de erros
├── componentes/    peças de interface reutilizáveis e sem regra de negócio
├── contextos/      estado global: sessão autenticada e carrinho
├── ganchos/        acesso aos contextos e useRequisicao (carregando/erro/recarregar)
├── paginas/        uma por rota, orquestram api + componentes
├── tipos/          interfaces espelhando os DTOs do backend
├── utilitarios/    formatação de moeda, data e nota
├── estilos/        tokens de cor e classes compartilhadas
└── Rotas.tsx       mapa de rotas e proteção por papel
```

Princípios seguidos:

- **Componente não chama `axios`.** Ele usa uma função de `api/`, que devolve tipo conhecido.
- **Nenhum `any`.** Os tipos em `tipos/api.ts` espelham os DTOs do backend.
- **O token não é manipulado nas páginas.** O interceptor do axios injeta o cabeçalho
  `Authorization` e, em caso de 401, limpa a sessão e devolve o usuário ao login.
- **Erro da API vira mensagem para o usuário.** `mensagemDoErro` lê o corpo padronizado
  (`{ status, erro, mensagem, campos }`) do `GlobalExceptionHandler`.
- **Autorização espelha o backend.** `RotaProtegida` esconde as rotas por papel, mas quem
  decide de fato é a API: o front não é a camada de segurança.

## Telas

| Rota | Quem acessa | Conteúdo |
|---|---|---|
| `/` | público | catálogo com busca, filtro por categoria, ordenação e paginação |
| `/jogos/:id` | público | detalhe, preço promocional, avaliações, carrinho, lista de desejos |
| `/login`, `/cadastro` | público | autenticação; o cadastro cria jogador ou publicadora |
| `/carrinho` | autenticado | itens, saldo da carteira e pagamento |
| `/biblioteca` | autenticado | jogos adquiridos, horas jogadas, avaliação e lista de desejos |
| `/pedidos` | autenticado | histórico com itens, descontos e status |
| `/publicadora` | PUBLICADORA, ADMIN | jogos próprios, vendas, cadastro de jogo e promoções |
| `/admin` | ADMIN | usuários e categorias |
