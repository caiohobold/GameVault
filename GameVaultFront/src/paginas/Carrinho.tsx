import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { mensagemDoErro } from '../api/cliente';
import * as apiPedidos from '../api/pedidos';
import * as apiUsuarios from '../api/usuarios';
import { CapaJogo } from '../componentes/CapaJogo';
import { Mensagem } from '../componentes/Mensagem';
import { useAutenticacao } from '../ganchos/useAutenticacao';
import { useCarrinho } from '../ganchos/useCarrinho';
import { formatarMoeda } from '../utilitarios/formatos';

export function Carrinho() {
  const { itens, remover, limpar } = useCarrinho();
  const { usuario, atualizarUsuario } = useAutenticacao();
  const navegar = useNavigate();

  const [processando, setProcessando] = useState(false);
  const [erro, setErro] = useState<string | null>(null);

  const subtotal = itens.reduce((soma, jogo) => soma + Number(jogo.preco), 0);
  const saldo = Number(usuario?.saldo ?? 0);

  async function finalizar() {
    setProcessando(true);
    setErro(null);

    try {
      const pedido = await apiPedidos.criar(itens.map((jogo) => jogo.id));
      await apiPedidos.pagar(pedido.id);

      if (usuario) {
        atualizarUsuario(await apiUsuarios.buscarPorId(usuario.id));
      }

      limpar();
      navegar('/biblioteca', { state: { compraConcluida: true } });
    } catch (falha) {
      setErro(mensagemDoErro(falha, 'Não foi possível concluir a compra'));
    } finally {
      setProcessando(false);
    }
  }

  if (itens.length === 0) {
    return (
      <>
        <h1>Seu carrinho</h1>
        <p className="subtitulo">Nenhum jogo adicionado ainda</p>
        <div className="cartao vazio">
          Seu carrinho está vazio. <Link to="/" style={{ color: 'var(--roxo)' }}>Ver o catálogo</Link>
        </div>
      </>
    );
  }

  return (
    <>
      <h1>Seu carrinho</h1>
      <p className="subtitulo">
        {itens.length} {itens.length === 1 ? 'jogo' : 'jogos'} selecionados
      </p>

      <div className="duas-colunas">
        <div className="cartao painel">
          <h2>Itens</h2>

          {itens.map((jogo) => (
            <div key={jogo.id} className="linha-item">
              <div className="mini">
                <CapaJogo id={jogo.id} titulo={jogo.titulo} />
              </div>
              <div style={{ flex: 1 }}>
                <Link to={`/jogos/${jogo.id}`} style={{ fontWeight: 650 }}>
                  {jogo.titulo}
                </Link>
                <div style={{ fontSize: 12.5, color: 'var(--texto-fraco)' }}>
                  {jogo.categorias.map((categoria) => categoria.nome).join(' · ')}
                </div>
              </div>
              <div className="preco">{formatarMoeda(jogo.preco)}</div>
              <button type="button" className="botao perigo pequeno" onClick={() => remover(jogo.id)}>
                Remover
              </button>
            </div>
          ))}

          <div className="aviso" style={{ marginTop: 18 }}>
            O preço de cada item é congelado no momento da compra, e eventuais promoções são
            aplicadas pelo servidor ao fechar o pedido.
          </div>
        </div>

        <aside className="cartao painel">
          <h2>Resumo</h2>

          {erro && <Mensagem tipo="erro">{erro}</Mensagem>}

          <div className="total">
            <span style={{ color: 'var(--texto-fraco)' }}>Subtotal sem desconto</span>
            <span>{formatarMoeda(subtotal)}</span>
          </div>
          <div className="total grande">
            <span>Total estimado</span>
            <span>{formatarMoeda(subtotal)}</span>
          </div>

          <div style={{ margin: '16px 0', padding: 13, background: 'var(--superficie-2)', borderRadius: 8 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13.5 }}>
              <span style={{ color: 'var(--texto-fraco)' }}>Saldo na carteira</span>
              <b style={{ color: 'var(--verde)' }}>{formatarMoeda(saldo)}</b>
            </div>
          </div>

          <button type="button" className="botao largo" onClick={finalizar} disabled={processando}>
            {processando ? 'Processando...' : 'Comprar e pagar com saldo'}
          </button>

          <p style={{ fontSize: 12.5, color: 'var(--texto-fraco)', marginTop: 14, textAlign: 'center' }}>
            Os jogos entram na sua biblioteca assim que o pagamento é confirmado.
          </p>
        </aside>
      </div>
    </>
  );
}
