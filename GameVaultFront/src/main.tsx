import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { ProvedorAutenticacao } from './contextos/AutenticacaoContexto';
import { ProvedorCarrinho } from './contextos/CarrinhoContexto';
import { Rotas } from './Rotas';
import './estilos/global.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ProvedorAutenticacao>
      <ProvedorCarrinho>
        <Rotas />
      </ProvedorCarrinho>
    </ProvedorAutenticacao>
  </StrictMode>,
);
