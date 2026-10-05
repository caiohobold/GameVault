import { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { mensagemDoErro } from '../api/cliente';
import { Mensagem } from '../componentes/Mensagem';
import { useAutenticacao } from '../ganchos/useAutenticacao';

interface EstadoNavegacao {
  de?: string;
}

export function Login() {
  const { entrar, carregando } = useAutenticacao();
  const navegar = useNavigate();
  const localizacao = useLocation();

  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [erro, setErro] = useState<string | null>(null);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro(null);

    try {
      await entrar({ email, senha });
      const destino = (localizacao.state as EstadoNavegacao | null)?.de ?? '/';
      navegar(destino, { replace: true });
    } catch (falha) {
      setErro(mensagemDoErro(falha, 'Não foi possível entrar'));
    }
  }

  return (
    <div className="centralizado">
      <div className="marca-grande">
        <div className="cofre">G</div>
        <div className="nome">
          Game<span>Vault</span>
        </div>
        <p>Sua biblioteca de jogos, em um só lugar</p>
      </div>

      <form className="cartao painel" onSubmit={enviar}>
        <h2>Entrar</h2>

        {erro && <Mensagem tipo="erro">{erro}</Mensagem>}

        <div className="campo">
          <label htmlFor="email">E-mail</label>
          <input
            id="email"
            type="email"
            required
            value={email}
            onChange={(evento) => setEmail(evento.target.value)}
            placeholder="voce@email.com"
          />
        </div>

        <div className="campo">
          <label htmlFor="senha">Senha</label>
          <input
            id="senha"
            type="password"
            required
            value={senha}
            onChange={(evento) => setSenha(evento.target.value)}
          />
        </div>

        <button type="submit" className="botao largo" disabled={carregando}>
          {carregando ? 'Entrando...' : 'Entrar'}
        </button>
      </form>

      <p className="alternar">
        Não tem conta? <Link to="/cadastro">Criar conta</Link>
      </p>
    </div>
  );
}
