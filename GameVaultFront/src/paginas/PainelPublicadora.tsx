import { useEffect, useState } from 'react';
import { mensagemDoErro } from '../api/cliente';
import * as apiCategorias from '../api/categorias';
import * as apiJogos from '../api/jogos';
import * as apiPromocoes from '../api/promocoes';
import { Carregando } from '../componentes/Carregando';
import { Mensagem } from '../componentes/Mensagem';
import { useAutenticacao } from '../ganchos/useAutenticacao';
import { useRequisicao } from '../ganchos/useRequisicao';
import { formatarMoeda, formatarNota } from '../utilitarios/formatos';
import type { Categoria } from '../tipos/api';

export function PainelPublicadora() {
  const { usuario, temPapel } = useAutenticacao();

  const jogos = useRequisicao(() => apiJogos.listarMeus(), []);
  const vendas = useRequisicao(() => apiJogos.resumirVendas(), []);

  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [titulo, setTitulo] = useState('');
  const [descricao, setDescricao] = useState('');
  const [preco, setPreco] = useState('');
  const [dataLancamento, setDataLancamento] = useState('');
  const [categoriasSelecionadas, setCategoriasSelecionadas] = useState<number[]>([]);
  const [avisoJogo, setAvisoJogo] = useState<{ tipo: 'erro' | 'sucesso'; texto: string } | null>(null);

  const [jogoPromocao, setJogoPromocao] = useState<number | ''>('');
  const [percentual, setPercentual] = useState('50');
  const [inicio, setInicio] = useState('');
  const [fim, setFim] = useState('');
  const [avisoPromocao, setAvisoPromocao] = useState<{ tipo: 'erro' | 'sucesso'; texto: string } | null>(null);

  useEffect(() => {
    apiCategorias.listar().then(setCategorias).catch(() => setCategorias([]));
  }, []);

  async function publicarJogo(evento: React.FormEvent) {
    evento.preventDefault();
    setAvisoJogo(null);

    try {
      await apiJogos.criar({
        titulo,
        descricao,
        preco: Number(preco),
        dataLancamento: dataLancamento || null,
        publicadoraId: temPapel('ADMIN') ? usuario?.id : undefined,
        categoriaIds: categoriasSelecionadas,
      });

      setAvisoJogo({ tipo: 'sucesso', texto: 'Jogo publicado no catálogo.' });
      setTitulo('');
      setDescricao('');
      setPreco('');
      setDataLancamento('');
      setCategoriasSelecionadas([]);
      jogos.recarregar();
    } catch (falha) {
      setAvisoJogo({ tipo: 'erro', texto: mensagemDoErro(falha) });
    }
  }

  async function criarPromocao(evento: React.FormEvent) {
    evento.preventDefault();
    setAvisoPromocao(null);

    if (jogoPromocao === '') {
      return;
    }

    try {
      await apiPromocoes.criar({
        jogoId: Number(jogoPromocao),
        percentualDesconto: Number(percentual),
        dataInicio: `${inicio}T00:00:00`,
        dataFim: `${fim}T23:59:59`,
      });

      setAvisoPromocao({ tipo: 'sucesso', texto: 'Promoção ativada.' });
    } catch (falha) {
      setAvisoPromocao({ tipo: 'erro', texto: mensagemDoErro(falha) });
    }
  }

  async function alternarAtivo(id: number, ativo: boolean) {
    try {
      if (ativo) {
        await apiJogos.desativar(id);
      } else {
        await apiJogos.reativar(id);
      }

      jogos.recarregar();
    } catch (falha) {
      window.alert(mensagemDoErro(falha));
    }
  }

  const totalArrecadado =
    vendas.dados?.content.reduce((soma, venda) => soma + Number(venda.valorArrecadado), 0) ?? 0;
  const totalVendido =
    vendas.dados?.content.reduce((soma, venda) => soma + Number(venda.quantidadeVendida), 0) ?? 0;

  return (
    <>
      <h1>{usuario?.nome}</h1>
      <p className="subtitulo">Painel da publicadora</p>

      <div className="tres-colunas" style={{ marginBottom: 26 }}>
        <div className="cartao indicador">
          <div className="valor">{jogos.dados?.totalElements ?? 0}</div>
          <div className="rotulo">Jogos publicados</div>
        </div>
        <div className="cartao indicador">
          <div className="valor" style={{ color: 'var(--verde)' }}>{formatarMoeda(totalArrecadado)}</div>
          <div className="rotulo">Faturamento total</div>
        </div>
        <div className="cartao indicador">
          <div className="valor">{totalVendido}</div>
          <div className="rotulo">Cópias vendidas</div>
        </div>
      </div>

      <div className="duas-colunas">
        <div className="cartao painel">
          <h2>Meus jogos</h2>

          {jogos.carregando && <Carregando />}
          {jogos.erro && <Mensagem tipo="erro">{jogos.erro}</Mensagem>}

          {jogos.dados && jogos.dados.content.length === 0 && (
            <div className="vazio">Você ainda não publicou nenhum jogo.</div>
          )}

          {jogos.dados && jogos.dados.content.length > 0 && (
            <table>
              <thead>
                <tr>
                  <th>Jogo</th>
                  <th>Preço</th>
                  <th>Vendas</th>
                  <th>Nota</th>
                  <th>Status</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {jogos.dados.content.map((jogo) => {
                  const venda = vendas.dados?.content.find((item) => item.jogoId === jogo.id);

                  return (
                    <tr key={jogo.id}>
                      <td><b>{jogo.titulo}</b></td>
                      <td>{formatarMoeda(jogo.preco)}</td>
                      <td>{venda?.quantidadeVendida ?? 0}</td>
                      <td className="nota">{formatarNota(jogo.notaMedia)}</td>
                      <td>
                        <span
                          className={`etiqueta-papel ${jogo.ativo ? 'papel-USUARIO' : 'papel-ADMIN'}`}
                        >
                          {jogo.ativo ? 'Ativo' : 'Inativo'}
                        </span>
                      </td>
                      <td style={{ textAlign: 'right' }}>
                        <button
                          type="button"
                          className="botao secundario pequeno"
                          onClick={() => alternarAtivo(jogo.id, jogo.ativo)}
                        >
                          {jogo.ativo ? 'Desativar' : 'Reativar'}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}

          <div className="aviso" style={{ marginTop: 16 }}>
            Uma publicadora só enxerga e edita os próprios jogos. Desativar não apaga o registro:
            pedidos antigos continuam válidos.
          </div>
        </div>

        <aside>
          <form className="cartao painel" style={{ marginBottom: 16 }} onSubmit={publicarJogo}>
            <h2>Cadastrar jogo</h2>

            {avisoJogo && <Mensagem tipo={avisoJogo.tipo}>{avisoJogo.texto}</Mensagem>}

            <div className="campo">
              <label htmlFor="titulo">Título</label>
              <input
                id="titulo"
                required
                maxLength={150}
                value={titulo}
                onChange={(evento) => setTitulo(evento.target.value)}
              />
            </div>
            <div className="campo">
              <label htmlFor="descricao">Descrição</label>
              <textarea
                id="descricao"
                rows={3}
                maxLength={2000}
                value={descricao}
                onChange={(evento) => setDescricao(evento.target.value)}
              />
            </div>
            <div className="campo">
              <label htmlFor="preco">Preço</label>
              <input
                id="preco"
                type="number"
                min="0"
                step="0.01"
                required
                value={preco}
                onChange={(evento) => setPreco(evento.target.value)}
              />
            </div>
            <div className="campo">
              <label htmlFor="lancamento">Data de lançamento</label>
              <input
                id="lancamento"
                type="date"
                value={dataLancamento}
                onChange={(evento) => setDataLancamento(evento.target.value)}
              />
            </div>
            <div className="campo">
              <label htmlFor="categorias">Categorias</label>
              <select
                id="categorias"
                multiple
                size={5}
                required
                value={categoriasSelecionadas.map(String)}
                onChange={(evento) =>
                  setCategoriasSelecionadas(
                    Array.from(evento.target.selectedOptions, (opcao) => Number(opcao.value)),
                  )
                }
              >
                {categorias.map((categoria) => (
                  <option key={categoria.id} value={categoria.id}>
                    {categoria.nome}
                  </option>
                ))}
              </select>
            </div>

            <button type="submit" className="botao largo">
              Publicar jogo
            </button>
          </form>

          <form className="cartao painel" onSubmit={criarPromocao}>
            <h2>Criar promoção</h2>

            {avisoPromocao && <Mensagem tipo={avisoPromocao.tipo}>{avisoPromocao.texto}</Mensagem>}

            <div className="campo">
              <label htmlFor="jogo-promocao">Jogo</label>
              <select
                id="jogo-promocao"
                required
                value={jogoPromocao}
                onChange={(evento) => setJogoPromocao(Number(evento.target.value))}
              >
                <option value="">Selecione um jogo</option>
                {jogos.dados?.content.map((jogo) => (
                  <option key={jogo.id} value={jogo.id}>
                    {jogo.titulo}
                  </option>
                ))}
              </select>
            </div>
            <div className="campo">
              <label htmlFor="percentual">Desconto (%)</label>
              <input
                id="percentual"
                type="number"
                min="1"
                max="90"
                required
                value={percentual}
                onChange={(evento) => setPercentual(evento.target.value)}
              />
            </div>
            <div className="campo">
              <label htmlFor="inicio">Início</label>
              <input
                id="inicio"
                type="date"
                required
                value={inicio}
                onChange={(evento) => setInicio(evento.target.value)}
              />
            </div>
            <div className="campo">
              <label htmlFor="fim">Fim</label>
              <input
                id="fim"
                type="date"
                required
                value={fim}
                onChange={(evento) => setFim(evento.target.value)}
              />
            </div>

            <button type="submit" className="botao largo">
              Ativar promoção
            </button>
          </form>
        </aside>
      </div>
    </>
  );
}
