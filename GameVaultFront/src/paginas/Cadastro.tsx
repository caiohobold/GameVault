import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { mensagemDoErro } from '../api/cliente';
import { Mensagem } from '../componentes/Mensagem';
import { useAutenticacao } from '../ganchos/useAutenticacao';
import type { Papel } from '../tipos/api';

export function Cadastro() {
  const { cadastrar, carregando } = useAutenticacao();
  const navegar = useNavigate();

  const [nome, setNome] = useState('');
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [papel, setPapel] = useState<Exclude<Papel, 'ADMIN'>>('USUARIO');
  const [erro, setErro] = useState<string | null>(null);

  async function enviar(evento: React.FormEvent) {
    evento.preventDefault();
    setErro(null);

    try {
      await cadastrar({ nome, email, senha, role: papel });
      navegar('/', { replace: true });
    } catch (falha) {
      setErro(mensagemDoErro(falha, 'Não foi possível criar a conta'));
    }
  }

  return (
    <div className="centralizado">
      <div className="marca-grande">
        <div className="cofre">G</div>
        <div className="nome">
          Game<span>Vault</span>
        </div>
        <p>Crie sua conta gratuitamente</p>
      </div>

      <form className="cartao painel" onSubmit={enviar}>
        <h2>Criar conta</h2>

        {erro && <Mensagem tipo="erro">{erro}</Mensagem>}

        <div className="campo">
          <label htmlFor="nome">Nome</label>
          <input
            id="nome"
            required
            minLength={3}
            value={nome}
            onChange={(evento) => setNome(evento.target.value)}
            placeholder="Seu nome completo"
          />
        </div>

        <div className="campo">
          <label htmlFor="email-cadastro">E-mail</label>
          <input
            id="email-cadastro"
            type="email"
            required
            value={email}
            onChange={(evento) => setEmail(evento.target.value)}
            placeholder="voce@email.com"
          />
        </div>

        <div className="campo">
          <label htmlFor="senha-cadastro">Senha</label>
          <input
            id="senha-cadastro"
            type="password"
            required
            minLength={6}
            value={senha}
            onChange={(evento) => setSenha(evento.target.value)}
            placeholder="Mínimo de 6 caracteres"
          />
        </div>

        <div className="campo">
          <label htmlFor="papel">Tipo de conta</label>
          <select
            id="papel"
            value={papel}
            onChange={(evento) => setPapel(evento.target.value as Exclude<Papel, 'ADMIN'>)}
          >
            <option value="USUARIO">Jogador — comprar e avaliar jogos</option>
            <option value="PUBLICADORA">Publicadora — publicar e vender jogos</option>
          </select>
        </div>

        <button type="submit" className="botao largo" disabled={carregando}>
          {carregando ? 'Criando...' : 'Criar conta'}
        </button>
      </form>

      <p className="alternar">
        Já tem conta? <Link to="/login">Entrar</Link>
      </p>
    </div>
  );
}
