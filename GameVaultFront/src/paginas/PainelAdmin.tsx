import { useEffect, useState } from 'react';
import { mensagemDoErro } from '../api/cliente';
import * as apiCategorias from '../api/categorias';
import * as apiUsuarios from '../api/usuarios';
import { Carregando } from '../componentes/Carregando';
import { Mensagem } from '../componentes/Mensagem';
import { useRequisicao } from '../ganchos/useRequisicao';
import { formatarMoeda } from '../utilitarios/formatos';
import type { Categoria, Papel } from '../tipos/api';

export function PainelAdmin() {
  const [nome, setNome] = useState('');
  const [nomeAplicado, setNomeAplicado] = useState('');
  const [papel, setPapel] = useState<Papel | ''>('');

  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [novaCategoria, setNovaCategoria] = useState('');
  const [descricaoCategoria, setDescricaoCategoria] = useState('');
  const [aviso, setAviso] = useState<{ tipo: 'erro' | 'sucesso'; texto: string } | null>(null);

  const usuarios = useRequisicao(
    () => apiUsuarios.listar(nomeAplicado || undefined, papel === '' ? undefined : papel),
    [nomeAplicado, papel],
  );

  function carregarCategorias() {
    apiCategorias.listar().then(setCategorias).catch(() => setCategorias([]));
  }

  useEffect(carregarCategorias, []);

  async function alternarAtivo(id: number, ativo: boolean) {
    try {
      await apiUsuarios.alterarAtivo(id, !ativo);
      usuarios.recarregar();
    } catch (falha) {
      window.alert(mensagemDoErro(falha));
    }
  }

  async function adicionarCategoria(evento: React.FormEvent) {
    evento.preventDefault();
    setAviso(null);

    try {
      await apiCategorias.criar({ nome: novaCategoria, descricao: descricaoCategoria });
      setNovaCategoria('');
      setDescricaoCategoria('');
      setAviso({ tipo: 'sucesso', texto: 'Categoria criada.' });
      carregarCategorias();
    } catch (falha) {
      setAviso({ tipo: 'erro', texto: mensagemDoErro(falha) });
    }
  }

  async function removerCategoria(id: number) {
    try {
      await apiCategorias.excluir(id);
      carregarCategorias();
    } catch (falha) {
      window.alert(mensagemDoErro(falha));
    }
  }

  return (
    <>
      <h1>Administração</h1>
      <p className="subtitulo">Gestão da plataforma</p>

      <div className="tres-colunas" style={{ marginBottom: 26 }}>
        <div className="cartao indicador">
          <div className="valor">{usuarios.dados?.totalElements ?? 0}</div>
          <div className="rotulo">Usuários cadastrados</div>
        </div>
        <div className="cartao indicador">
          <div className="valor">{categorias.length}</div>
          <div className="rotulo">Categorias</div>
        </div>
        <div className="cartao indicador">
          <div className="valor" style={{ color: 'var(--verde)' }}>
            {formatarMoeda(
              usuarios.dados?.content.reduce((soma, usuario) => soma + Number(usuario.saldo), 0) ?? 0,
            )}
          </div>
          <div className="rotulo">Saldo total em carteiras</div>
        </div>
      </div>

      <div className="duas-colunas">
        <div className="cartao painel">
          <h2>Usuários</h2>

          <form
            className="filtros"
            onSubmit={(evento) => {
              evento.preventDefault();
              setNomeAplicado(nome.trim());
            }}
          >
            <input
              value={nome}
              onChange={(evento) => setNome(evento.target.value)}
              placeholder="Buscar por nome..."
              aria-label="Buscar por nome"
            />
            <select
              value={papel}
              onChange={(evento) => setPapel(evento.target.value as Papel | '')}
              aria-label="Filtrar por papel"
            >
              <option value="">Todos os papéis</option>
              <option value="ADMIN">Admin</option>
              <option value="PUBLICADORA">Publicadora</option>
              <option value="USUARIO">Usuário</option>
            </select>
            <button type="submit" className="botao">
              Filtrar
            </button>
          </form>

          {usuarios.carregando && <Carregando />}
          {usuarios.erro && <Mensagem tipo="erro">{usuarios.erro}</Mensagem>}

          {usuarios.dados && (
            <table>
              <thead>
                <tr>
                  <th>Nome</th>
                  <th>E-mail</th>
                  <th>Papel</th>
                  <th>Saldo</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {usuarios.dados.content.map((usuario) => (
                  <tr key={usuario.id}>
                    <td><b>{usuario.nome}</b></td>
                    <td style={{ color: 'var(--texto-fraco)' }}>{usuario.email}</td>
                    <td>
                      <span className={`etiqueta-papel papel-${usuario.role}`}>{usuario.role}</span>
                    </td>
                    <td>{formatarMoeda(usuario.saldo)}</td>
                    <td style={{ color: usuario.ativo ? undefined : 'var(--vermelho)' }}>
                      {usuario.ativo ? 'Ativo' : 'Desativado'}
                    </td>
                    <td style={{ textAlign: 'right' }}>
                      <button
                        type="button"
                        className="botao secundario pequeno"
                        onClick={() => alternarAtivo(usuario.id, usuario.ativo)}
                      >
                        {usuario.ativo ? 'Desativar' : 'Reativar'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        <aside className="cartao painel">
          <h2>Categorias</h2>

          {aviso && <Mensagem tipo={aviso.tipo}>{aviso.texto}</Mensagem>}

          <form onSubmit={adicionarCategoria} style={{ marginBottom: 18 }}>
            <div className="campo">
              <label htmlFor="nova-categoria">Nome</label>
              <input
                id="nova-categoria"
                required
                maxLength={60}
                value={novaCategoria}
                onChange={(evento) => setNovaCategoria(evento.target.value)}
              />
            </div>
            <div className="campo">
              <label htmlFor="descricao-categoria">Descrição</label>
              <input
                id="descricao-categoria"
                maxLength={255}
                value={descricaoCategoria}
                onChange={(evento) => setDescricaoCategoria(evento.target.value)}
              />
            </div>
            <button type="submit" className="botao largo">
              Criar categoria
            </button>
          </form>

          <table>
            <tbody>
              {categorias.map((categoria) => (
                <tr key={categoria.id}>
                  <td>{categoria.nome}</td>
                  <td style={{ textAlign: 'right' }}>
                    <button
                      type="button"
                      className="botao perigo pequeno"
                      onClick={() => removerCategoria(categoria.id)}
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </aside>
      </div>
    </>
  );
}
