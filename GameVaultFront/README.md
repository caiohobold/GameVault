# GameVaultFront

Frontend do GameVault — **React + Vite**.

## Quando chegar a Fase 6

```bash
npm create vite@latest GameVaultFront -- --template react
cd GameVaultFront
npm install
npm run dev
```

A API roda em `http://localhost:8080` e o Vite em `http://localhost:5173`
(origem já liberada no CORS do backend via `CORS_ORIGINS`).

## Telas previstas

- Login e cadastro (token JWT guardado e enviado no header `Authorization`)
- Catálogo de jogos com busca e filtro por categoria
- Detalhe do jogo (descrição, preço, promoção, avaliações)
- Carrinho e checkout
- Biblioteca do usuário
- Painel da publicadora
