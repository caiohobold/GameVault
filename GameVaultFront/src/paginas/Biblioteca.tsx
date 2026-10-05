import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { mensagemDoErro } from '../api/cliente';
import * as apiAvaliacoes from '../api/avaliacoes';
import * as apiBiblioteca from '../api/biblioteca';
import * as apiListaDesejos from '../api/listaDesejos';
import { CapaJogo } from '../componentes/CapaJogo';
import { Carregando } from '../componentes/Carregando';
import { Mensagem } from '../componentes/Mensagem';
import { useRequisicao } from '../ganchos/useRequisicao';
import { formatarData } from '../utilitarios/formatos';

type Aba = 'jogos' | 'desejos';

export function Biblioteca() {
  const localizacao = useLocation();
  const compraConcluida = (localizacao.state as { compraConcluida?: boolean } | null)?.compraConcluida;

  const [aba, setAba] = useState<Aba>('jogos');
  const [jogoAvaliado, setJogoAvaliado] = useState<number | ''>('');
  const [nota, setNota] = useState(5);
  const [comentario, setComentario] = useState('');
  const [aviso, setAviso] = useState<{ tipo: 'erro' | 'sucesso'; texto: string } | null>(null);

  const biblioteca = useRequisicao(() => apiBiblioteca.listarMinha(), []);
  const desejos = useRequisicao(() => apiListaDesejos.listar(), []);

  async function enviarAvaliacao(evento: React.FormEvent) {
    evento.preventDefault();
    setAviso(null);

    if (jogoAvaliado === '') {
      return;
    }

    try {
      await apiAvaliacoes.criar({ jogoId: Number(jogoAvaliado), nota, comentario });
      setAviso({ tipo: 'sucesso', texto: 'Avaliação publicada. A nota média do jogo foi atualizada.' });
      setComentario('');
      biblioteca.recarregar();
    } catch (falha) {
      setAviso({ tipo: 'erro', texto: mensagemDoErro(falha) });
    }
  }

  async function removerDesejo(id: number) {
    await apiListaDesejos.remover(id);
    desejos.recarregar();
  }

  async function registrarHoras(id: number, atual: number) {
    await apiBiblioteca.registrarHoras(id, atual + 1);
    biblioteca.recarregar();
  }

  const totalHoras =
    biblioteca.dados?.content.reduce((soma, item) => soma + item.horasJogadas, 0) ?? 0;

  return (
    <>
      <h1>Minha biblioteca</h1>
      <p className="subtitulo">
        {biblioteca.dados?.totalElements ?? 0} jogos · {totalHoras} horas jogadas no total
      </p>

      {compraConcluida && (
        <Mensagem tipo="sucesso">Compra concluída. Os jogos já estão na sua biblioteca.</Mensagem>
      )}

      <div className="abas">
        <button
          type="button"
          className={aba === 'jogos' ? 'ativa' : ''}
          onClick={() => setAba('jogos')}
        >
          Meus jogos ({biblioteca.dados?.totalElements ?? 0})
        </button>
        <button
          type="button"
          className={aba === 'desejos' ? 'ativa' : ''}
          onClick={() => setAba('desejos')}
        >
          Lista de desejos ({desejos.dados?.totalElements ?? 0})
        </button>
      </div>

      {aba === 'jogos' && (
        <>
          {biblioteca.carregando && <Carregando />}
          {biblioteca.erro && <Mensagem tipo="erro">{biblioteca.erro}</Mensagem>}

          {biblioteca.dados && biblioteca.dados.content.length === 0 && (
            <div className="cartao vazio">
              Você ainda não tem jogos. <Link to="/" style={{ color: 'var(--roxo)' }}>Ver o catálogo</Link>
            </div>
          )}

          {biblioteca.dados && biblioteca.dados.content.length > 0 && (
            <div className="grade">
              {biblioteca.dados.content.map((item) => (
                <div key={item.id} className="cartao-jogo">
                  <CapaJogo id={item.jogo.id} titulo={item.jogo.titulo} />
                  <div className="corpo">
                    <div className="titulo">{item.jogo.titulo}</div>
                    <div className="categorias">
                      Adquirido em {formatarData(item.dataAquisicao)}
                    </div>
                    <div style={{ fontSize: 12.5, color: 'var(--texto-fraco)' }}>
                      {item.horasJogadas} horas jogadas
                    </div>
                    <div className="barra-horas">
                      <div style={{ width: `${Math.min(item.horasJogadas * 2, 100)}%` }} />
                    </div>
                    <button
                      type="button"
                      className="botao pequeno"
                      style={{ marginTop: 12, width: '100%' }}
                      onClick={() => registrarHoras(item.id, item.horasJogadas)}
                    >
                      Registrar 1 hora
                    </button>
                  </div>
                </div>
              ))}
            </div>
          )}

          {biblioteca.dados && biblioteca.dados.content.length > 0 && (
            <form className="cartao painel" style={{ marginTop: 26 }} onSubmit={enviarAvaliacao}>
              <h2>Avaliar um jogo da sua biblioteca</h2>

              {aviso && <Mensagem tipo={aviso.tipo}>{aviso.texto}</Mensagem>}

              <div className="duas-colunas">
                <div>
                  <div className="campo">
                    <label htmlFor="jogo-avaliado">Jogo</label>
                    <select
                      id="jogo-avaliado"
                      required
                      value={jogoAvaliado}
                      onChange={(evento) => setJogoAvaliado(Number(evento.target.value))}
                    >
                      <option value="">Selecione um jogo</option>
                      {biblioteca.dados.content.map((item) => (
                        <option key={item.id} value={item.jogo.id}>
                          {item.jogo.titulo}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div className="campo">
                    <label htmlFor="nota">Nota</label>
                    <select
                      id="nota"
                      value={nota}
                      onChange={(evento) => setNota(Number(evento.target.value))}
                    >
                      {[5, 4, 3, 2, 1].map((valor) => (
                        <option key={valor} value={valor}>
                          {'★'.repeat(valor)}
                          {'☆'.repeat(5 - valor)} — {valor}
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
                <div>
                  <div className="campo">
                    <label htmlFor="comentario">Comentário (opcional)</label>
                    <textarea
                      id="comentario"
                      rows={4}
                      maxLength={1000}
                      value={comentario}
                      onChange={(evento) => setComentario(evento.target.value)}
                      placeholder="O que você achou do jogo?"
                    />
                  </div>
                  <button type="submit" className="botao">
                    Publicar avaliação
                  </button>
                </div>
              </div>
            </form>
          )}
        </>
      )}

      {aba === 'desejos' && (
        <>
          {desejos.carregando && <Carregando />}
          {desejos.erro && <Mensagem tipo="erro">{desejos.erro}</Mensagem>}

          {desejos.dados && desejos.dados.content.length === 0 && (
            <div className="cartao vazio">Sua lista de desejos está vazia.</div>
          )}

          {desejos.dados && desejos.dados.content.length > 0 && (
            <div className="cartao painel">
              {desejos.dados.content.map((item) => (
                <div key={item.id} className="linha-item">
                  <div className="mini">
                    <CapaJogo id={item.jogo.id} titulo={item.jogo.titulo} />
                  </div>
                  <div style={{ flex: 1 }}>
                    <Link to={`/jogos/${item.jogo.id}`} style={{ fontWeight: 650 }}>
                      {item.jogo.titulo}
                    </Link>
                    <div style={{ fontSize: 12.5, color: 'var(--texto-fraco)' }}>
                      Adicionado em {formatarData(item.dataAdicao)}
                    </div>
                  </div>
                  <button
                    type="button"
                    className="botao perigo pequeno"
                    onClick={() => removerDesejo(item.id)}
                  >
                    Remover
                  </button>
                </div>
              ))}
            </div>
          )}
        </>
      )}
    </>
  );
}
