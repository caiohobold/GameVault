import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useAutenticacao } from '../ganchos/useAutenticacao';
import { useCarrinho } from '../ganchos/useCarrinho';
import { formatarMoeda } from '../utilitarios/formatos';

function iniciais(nome: string): string {
  return nome
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map((parte) => parte[0])
    .join('')
    .toUpperCase();
}

export function Cabecalho() {
  const { usuario, autenticado, sair, temPapel } = useAutenticacao();
  const { quantidade } = useCarrinho();
  const navegar = useNavigate();

  function encerrarSessao() {
    sair();
    navegar('/login');
  }

  return (
    <header className="cabecalho">
      <div className="interno">
        <Link to="/" className="marca">
          <span className="cofre">G</span>
          Game<span className="destaque">Vault</span>
        </Link>

        <nav className="menu">
          <NavLink to="/">Catálogo</NavLink>
          {autenticado && <NavLink to="/biblioteca">Biblioteca</NavLink>}
          {autenticado && <NavLink to="/pedidos">Pedidos</NavLink>}
          {temPapel('PUBLICADORA', 'ADMIN') && <NavLink to="/publicadora">Meus jogos</NavLink>}
          {temPapel('ADMIN') && <NavLink to="/admin">Administração</NavLink>}
        </nav>

        <div className="direita">
          {autenticado ? (
            <>
              <Link to="/carrinho" className="carrinho-link">
                Carrinho{quantidade > 0 && <span className="contador">{quantidade}</span>}
              </Link>
              <div className="carteira">
                Saldo: <b>{formatarMoeda(usuario?.saldo ?? 0)}</b>
              </div>
              <div className="avatar" title={usuario?.nome}>
                {iniciais(usuario?.nome ?? '?')}
              </div>
              <button type="button" className="botao secundario pequeno" onClick={encerrarSessao}>
                Sair
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="botao secundario pequeno">
                Entrar
              </Link>
              <Link to="/cadastro" className="botao pequeno">
                Criar conta
              </Link>
            </>
          )}
        </div>
      </div>
    </header>
  );
}
