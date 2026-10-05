import { useEffect, useMemo, useState } from 'react';
import * as apiCategorias from '../api/categorias';
import * as apiJogos from '../api/jogos';
import * as apiPromocoes from '../api/promocoes';
import { Carregando } from '../componentes/Carregando';
import { CartaoJogo } from '../componentes/CartaoJogo';
import { Mensagem } from '../componentes/Mensagem';
import { Paginacao } from '../componentes/Paginacao';
import { useRequisicao } from '../ganchos/useRequisicao';
import type { Categoria, Promocao } from '../tipos/api';

const TAMANHO_PAGINA = 12;

export function Catalogo() {
  const [busca, setBusca] = useState('');
  const [buscaAplicada, setBuscaAplicada] = useState('');
  const [categoriaId, setCategoriaId] = useState<number | ''>('');
  const [ordenacao, setOrdenacao] = useState('titulo,asc');
  const [pagina, setPagina] = useState(0);

  const [categorias, setCategorias] = useState<Categoria[]>([]);
  const [promocoes, setPromocoes] = useState<Promocao[]>([]);

  useEffect(() => {
    apiCategorias
      .listar()
      .then(setCategorias)
      .catch(() => setCategorias([]));

    apiPromocoes
      .listarVigentes()
      .then(setPromocoes)
      .catch(() => setPromocoes([]));
  }, []);

  const descontoPorJogo = useMemo(() => {
    const mapa = new Map<number, number>();
    promocoes.forEach((promocao) => mapa.set(promocao.jogo.id, promocao.percentualDesconto));
    return mapa;
  }, [promocoes]);

  const { dados, carregando, erro } = useRequisicao(
    () =>
      apiJogos.listar({
        titulo: buscaAplicada || undefined,
        categoriaId: categoriaId === '' ? undefined : categoriaId,
        page: pagina,
        size: TAMANHO_PAGINA,
        sort: ordenacao,
      }),
    [buscaAplicada, categoriaId, ordenacao, pagina],
  );

  function aplicarBusca(evento: React.FormEvent) {
    evento.preventDefault();
    setPagina(0);
    setBuscaAplicada(busca.trim());
  }

  return (
    <>
      <h1>Catálogo</h1>
      <p className="subtitulo">
        {dados ? `${dados.totalElements} jogos disponíveis` : 'Carregando catálogo...'}
      </p>

      <form className="filtros" onSubmit={aplicarBusca}>
        <input
          value={busca}
          onChange={(evento) => setBusca(evento.target.value)}
          placeholder="Buscar por título..."
          aria-label="Buscar por título"
        />
        <select
          value={categoriaId}
          onChange={(evento) => {
            setPagina(0);
            setCategoriaId(evento.target.value === '' ? '' : Number(evento.target.value));
          }}
          aria-label="Filtrar por categoria"
        >
          <option value="">Todas as categorias</option>
          {categorias.map((categoria) => (
            <option key={categoria.id} value={categoria.id}>
              {categoria.nome}
            </option>
          ))}
        </select>
        <select
          value={ordenacao}
          onChange={(evento) => {
            setPagina(0);
            setOrdenacao(evento.target.value);
          }}
          aria-label="Ordenar"
        >
          <option value="titulo,asc">Ordenar por título</option>
          <option value="preco,asc">Menor preço</option>
          <option value="preco,desc">Maior preço</option>
          <option value="notaMedia,desc">Melhor avaliados</option>
          <option value="dataLancamento,desc">Lançamentos</option>
        </select>
        <button type="submit" className="botao">
          Buscar
        </button>
      </form>

      {erro && <Mensagem tipo="erro">{erro}</Mensagem>}
      {carregando && <Carregando />}

      {dados && dados.content.length === 0 && (
        <div className="cartao vazio">Nenhum jogo encontrado com esses filtros.</div>
      )}

      {dados && dados.content.length > 0 && (
        <>
          <div className="grade">
            {dados.content.map((jogo) => (
              <CartaoJogo
                key={jogo.id}
                jogo={jogo}
                percentualDesconto={descontoPorJogo.get(jogo.id)}
              />
            ))}
          </div>
          <Paginacao paginaAtual={dados.number} totalPaginas={dados.totalPages} aoMudar={setPagina} />
        </>
      )}
    </>
  );
}
