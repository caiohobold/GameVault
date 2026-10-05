import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { mensagemDoErro } from '../api/cliente';
import * as apiJogos from '../api/jogos';
import * as apiListaDesejos from '../api/listaDesejos';
import * as apiPromocoes from '../api/promocoes';
import { CapaJogo } from '../componentes/CapaJogo';
import { Carregando } from '../componentes/Carregando';
import { EstrelasNota } from '../componentes/EstrelasNota';
import { Mensagem } from '../componentes/Mensagem';
import { PrecoJogo } from '../componentes/PrecoJogo';
import { useAutenticacao } from '../ganchos/useAutenticacao';
import { useCarrinho } from '../ganchos/useCarrinho';
import { useRequisicao } from '../ganchos/useRequisicao';
import { formatarData, formatarNota } from '../utilitarios/formatos';

export function DetalheJogo() {
  const { id } = useParams<{ id: string }>();
  const jogoId = Number(id);

  const { autenticado } = useAutenticacao();
  const { adicionar, contem } = useCarrinho();

  const [desconto, setDesconto] = useState<number | undefined>();
  const [aviso, setAviso] = useState<{ tipo: 'erro' | 'sucesso'; texto: string } | null>(null);

  const { dados: jogo, carregando, erro } = useRequisicao(() => apiJogos.buscarPorId(jogoId), [jogoId]);
  const { dados: avaliacoes } = useRequisicao(() => apiJogos.listarAvaliacoes(jogoId), [jogoId]);

  useEffect(() => {
    apiPromocoes
      .listarVigentes()
      .then((lista) => setDesconto(lista.find((item) => item.jogo.id === jogoId)?.percentualDesconto))
      .catch(() => setDesconto(undefined));
  }, [jogoId]);

  async function desejar() {
    setAviso(null);

    try {
      await apiListaDesejos.adicionar(jogoId);
      setAviso({ tipo: 'sucesso', texto: 'Jogo adicionado à sua lista de desejos.' });
    } catch (falha) {
      setAviso({ tipo: 'erro', texto: mensagemDoErro(falha) });
    }
  }

  if (carregando) {
    return <Carregando />;
  }

  if (erro || !jogo) {
    return <Mensagem tipo="erro">{erro ?? 'Jogo não encontrado'}</Mensagem>;
  }

  const noCarrinho = contem(jogo.id);

  return (
    <>
      <p className="subtitulo" style={{ marginBottom: 16 }}>
        <Link to="/" style={{ color: 'var(--texto-fraco)' }}>
          ← Voltar ao catálogo
        </Link>
      </p>

      <div className="duas-colunas">
        <div>
          <div style={{ marginBottom: 20 }}>
            <CapaJogo id={jogo.id} titulo={jogo.titulo} altura="16 / 8" fonte="27px" />
          </div>

          <h1>{jogo.titulo}</h1>
          <p className="subtitulo">
            {jogo.publicadora.nome} · Lançado em {formatarData(jogo.dataLancamento)} ·{' '}
            <EstrelasNota nota={jogo.notaMedia} total={jogo.totalAvaliacoes} />
          </p>

          <div className="cartao painel" style={{ marginBottom: 20 }}>
            <h2>Sobre o jogo</h2>
            <p style={{ color: '#c3c8da', fontSize: 14.5 }}>
              {jogo.descricao || 'Este jogo ainda não tem descrição.'}
            </p>
            <div style={{ marginTop: 14, display: 'flex', gap: 8, flexWrap: 'wrap' }}>
              {jogo.categorias.map((categoria) => (
                <span key={categoria.id} className="etiqueta-papel papel-PUBLICADORA">
                  {categoria.nome}
                </span>
              ))}
            </div>
          </div>

          <div className="cartao painel">
            <h2>Avaliações</h2>

            {!avaliacoes || avaliacoes.content.length === 0 ? (
              <p style={{ color: 'var(--texto-fraco)', fontSize: 14 }}>
                Este jogo ainda não recebeu avaliações.
              </p>
            ) : (
              avaliacoes.content.map((avaliacao) => (
                <article key={avaliacao.id} className="avaliacao">
                  <div className="topo">
                    <span className="autor">{avaliacao.usuario.nome}</span>
                    <span className="nota">★ {formatarNota(avaliacao.nota)}</span>
                    <span className="data">{formatarData(avaliacao.dataAvaliacao)}</span>
                  </div>
                  {avaliacao.comentario && <p>{avaliacao.comentario}</p>}
                </article>
              ))
            )}

            <div className="aviso" style={{ marginTop: 16 }}>
              Só quem tem o jogo na biblioteca consegue avaliar, e cada pessoa avalia uma vez por jogo.
            </div>
          </div>
        </div>

        <aside>
          <div className="cartao painel" style={{ marginBottom: 16 }}>
            {desconto && (
              <span className="selo-desconto" style={{ display: 'inline-block', marginBottom: 8 }}>
                Promoção -{desconto}%
              </span>
            )}

            <PrecoJogo preco={jogo.preco} percentualDesconto={desconto} tamanho={28} />

            <p style={{ color: 'var(--texto-fraco)', fontSize: 13, margin: '10px 0 16px' }}>
              {desconto ? 'Preço promocional calculado no servidor' : 'Sem promoção ativa no momento'}
            </p>

            {aviso && <Mensagem tipo={aviso.tipo}>{aviso.texto}</Mensagem>}

            <button
              type="button"
              className="botao largo"
              disabled={noCarrinho}
              onClick={() => adicionar(jogo)}
              style={{ marginBottom: 9 }}
            >
              {noCarrinho ? 'Já está no carrinho' : 'Adicionar ao carrinho'}
            </button>

            {autenticado && (
              <button type="button" className="botao secundario largo" onClick={desejar}>
                ♡ Lista de desejos
              </button>
            )}
          </div>

          <div className="cartao painel">
            <h2>Detalhes</h2>
            <table>
              <tbody>
                <tr>
                  <td style={{ color: 'var(--texto-fraco)' }}>Publicadora</td>
                  <td style={{ textAlign: 'right' }}>{jogo.publicadora.nome}</td>
                </tr>
                <tr>
                  <td style={{ color: 'var(--texto-fraco)' }}>Lançamento</td>
                  <td style={{ textAlign: 'right' }}>{formatarData(jogo.dataLancamento)}</td>
                </tr>
                <tr>
                  <td style={{ color: 'var(--texto-fraco)' }}>Nota média</td>
                  <td style={{ textAlign: 'right' }} className="nota">
                    {formatarNota(jogo.notaMedia)}
                  </td>
                </tr>
                <tr>
                  <td style={{ color: 'var(--texto-fraco)' }}>Avaliações</td>
                  <td style={{ textAlign: 'right' }}>{jogo.totalAvaliacoes}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </aside>
      </div>
    </>
  );
}
